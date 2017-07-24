package ee.evrcargo.imap;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static javax.mail.Folder.HOLDS_FOLDERS;

/**
 * Created by vadim.kimlaychuk on 10-Jul-17.
 */
public class Runner {
    private static NumberFormat nf = NumberFormat.getNumberInstance();
    private static long totalMessages = 0L;
    private static long totalSize = 0L;
    private static Configuration conf = new Configuration();
    private static Session session = Session.getInstance(conf, null);
    private static Store store;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        if (args.length == 0) {
            System.out.println("IMAP mailbox backup/restore util.");
            System.out.println("Edit build/resources/mbox.properties file before executing commands");
            System.out.println("NB! Folders with size = 0 are excluded from processing. Calendar folder considered to be unnecessary and also not processed.");
            System.out.println("BSD License. Written by VK Copyright (c) 2017 EVR Cargo.");
            System.out.println();
            System.out.println("-b  backup mailbox");
            System.out.println("-c  check mailbox structure and calculate size");
            System.out.println("-r  restore mailbox");
            System.out.println();
            System.out.println("Example: run.sh -c");
            System.exit(0);
        } else if (args[0].compareTo("-b") == 0) {
            System.out.println("Executing backup for mailbox " + conf.getProperty("mailbox.user") + " to backup/" + conf.getProperty("mailbox.domain") + "/" + conf.getProperty("mailbox.user"));
            backup();
        } else if (args[0].compareTo("-c") == 0) {
            System.out.println("Checking mailbox " + conf.getProperty("mailbox.user") + " at " + conf.getProperty("mailbox.domain"));
            check();
        } else if (args[0].compareTo("-r") == 0) {
            restore();
        } else {
            System.out.println("Unknown option");
        }
        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " sec.");
        System.exit(0);
    }

    /**
     * Connect to remote IMAP server and return root folder
     *
     * @return - root Folder
     */
    private static Folder getDefaultFolder() {
        try {
            store = session.getStore();
            store.connect(conf.getProperty("mailbox.user"), conf.getProperty("mailbox.password"));
            return store.getDefaultFolder();
        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static void restore() {
        try {
            System.out.println("Reading local folder structure...");
            String basePath = conf.getProperty("mailbox.restore.base");
            Folder rootImap = getDefaultFolder();
            if (rootImap == null) {
                System.out.println("No root IMAP folder! Exiting...");
                System.exit(-1);
            }
            restoreFolder(basePath);
            store.close();
        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Check folder to restore
     *
     * @param diskFolder - local folder name
     */
    private static void restoreFolder(String diskFolder) {
        try {
            // Create list of files at backup location
            List<String> existFiles = Files.walk(Paths.get(diskFolder), 1)
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().endsWith(".png"))
                    .filter(p -> !p.toString().contains("Calendar"))   // do not sync Calendars
                    .map(Path::toFile)
                    .map(File::getName)
                    .collect(Collectors.toList());

            if (existFiles.size() > 0) restoreMessages(diskFolder, existFiles);

            // Check if there are any subfolder to explore
            List<String> existFolders = Files.walk(Paths.get(diskFolder), 1)
                    .filter(Files::isDirectory)
                    .filter(p -> !p.toFile().getName().equals(diskFolder.substring(diskFolder.lastIndexOf("/") + 1)))
                    .map(Path::toFile)
                    .map(File::getName)
                    .collect(Collectors.toList());

            if (existFolders.size() > 0) {
                for (String folder : existFolders) {
                    restoreFolder(diskFolder + "/" + folder);
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Restore messages to IMAP folder from local disk copy
     *
     * @param diskFolder    - local folder
     * @param existingFiles - list of local filenames
     */
    private static void restoreMessages(String diskFolder, List<String> existingFiles) {
        try {
            String str = diskFolder.replaceFirst(conf.getProperty("mailbox.restore.base") + "/", "");
            Folder remote = store.getFolder(str);
            System.out.println("Reading remote folder: " + remote.getName() + "...");
            if (!remote.exists()) {
                System.out.println("Creating new folder at remote: " + remote.getFullName());
                remote.create(Folder.HOLDS_FOLDERS + Folder.HOLDS_MESSAGES);
            }
            remote.open(Folder.READ_WRITE);

            // Create list of message ID-s on the server
            List<String> msgIds = new ArrayList<>();
            Message[] messages = remote.getMessages();

            System.out.println("Getting server message id-s:");
            int i = 1;
            for (Message msg : messages) {
                System.out.print("Reading message " + i + "/" + messages.length + "\r");
                String[] id = msg.getHeader("Message-ID");
                msgIds.add(join(id));
                i++;
            }

            i = 1;
            int k = 0;
            for (String file : existingFiles) {
                System.out.print("Restoring message " + i + "/" + existingFiles.size() + "\r");
                Message msg = new MimeMessage(session, new FileInputStream(diskFolder + "/" + file));
                if (!msgIds.contains(join(msg.getHeader("Message-ID")))) {
                    remote.appendMessages(new Message[]{msg});
                    k++;
                }
                i++;
            }
            System.out.println();
            System.out.println("... from which " + k + " are new");
            remote.close(false);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private static String join(String[] ids) {
        StringBuffer buf = new StringBuffer();
        for (String id : ids) {
            buf.append(id);
        }
        return buf.toString();
    }

    private static void backup() {
        try {
            backupFolder(getDefaultFolder());
            System.out.println("\nBackup messages: " + totalMessages);
            System.out.println("Backup size: " + nf.format(totalSize) + " bytes");
            store.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private static void check() {
        try {
            System.out.println("Reading folder structure...");
            dumpFolder(getDefaultFolder(), "");
            System.out.println("\nTotal messages: " + totalMessages);
            System.out.println("Total size: " + nf.format(totalSize) + " bytes");
            store.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void backupFolder(Folder folder) throws Exception {
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0 && folder.getMessageCount() > 0 && !folder.getFullName().contentEquals("Calendar")) {  // skip Calendar
            totalMessages += folder.getMessageCount();
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }
            backupMessages(folder);
            folder.close(false);
        }

        // Check if there are any subfolder to explore
        if ((folder.getType() & HOLDS_FOLDERS) != 0) {
            Folder[] f = folder.list();
            for (Folder fld : f) backupFolder(fld);
        }
    }

    /**
     * Backup messages from IMAP folder to local drive.
     *
     * @param folder - remote IMAP folder
     * @throws MessagingException
     * @throws IOException
     */
    private static void backupMessages(Folder folder) throws MessagingException, IOException {
        UIDFolder uf = (UIDFolder) folder; // cast folder to UIDFolder interface
        String folderPath = "backup/" + conf.getProperty("mailbox.domain") + "/" + conf.getProperty("mailbox.user") + "/" + folder.getFullName() + "/";
        Files.createDirectories(Paths.get(folderPath));
        System.out.println("Saving folder: " + folder.getFullName());

        // Create list of the existing files to remove them from synchronization
        List<String> existingFiles = Files.walk(Paths.get(folderPath), 1)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .map(File::getName)
                .collect(Collectors.toList());

        Message[] messages = folder.getMessages();
        long size = 0L;
        int i = 1;
        for (Message msg : messages) {
            size = size + msg.getSize();
            try {
                System.out.print("Saving message " + i + "/" + folder.getMessageCount() + "\r");
                if (!existingFiles.contains(String.valueOf(uf.getUID(msg)))) {
                    File f = new File(folderPath + String.valueOf(uf.getUID(msg)));
                    OutputStream os = new FileOutputStream(f);
                    msg.writeTo(os);
                    os.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            i++;
        }
        System.out.println();
        totalSize += size;
    }

    /**
     * Show information about remote IMAP folder
     *
     * @param folder - IMAP folder
     * @param tab    - indentation
     * @throws Exception
     */
    private static void dumpFolder(Folder folder, String tab) throws Exception {
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0 && folder.getMessageCount() > 0) {
            System.out.println();
            System.out.println(tab + "Name:      " + folder.getName());
            System.out.println(tab + "Full Name: " + folder.getFullName());
            System.out.println(tab + "URL:       " + folder.getURLName());

            if (!folder.isSubscribed())
                System.out.println(tab + "Not Subscribed");

            if (folder.hasNewMessages()) System.out.println(tab + "Has New Messages");
            System.out.println(tab + "Messages:  " + folder.getMessageCount());
            totalMessages += folder.getMessageCount();
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }
            System.out.println(tab + "Messages size:    " + getSize(folder) + " bytes");
            folder.close(false);
        }

        // Check if there are any subfolder to explore
        if ((folder.getType() & HOLDS_FOLDERS) != 0) {
            Folder[] f = folder.list();
            for (Folder fld : f) dumpFolder(fld, tab + "    ");
        }
    }

    private static String getSize(Folder folder) throws MessagingException {
        Message[] messages = folder.getMessages();
        long size = 0L;
        for (Message msg : messages) size = size + msg.getSize();
        totalSize += size;
        return nf.format(size);
    }
}
