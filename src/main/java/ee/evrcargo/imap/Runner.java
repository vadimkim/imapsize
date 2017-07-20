package ee.evrcargo.imap;

import com.sun.mail.imap.IMAPFolder;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vadim.kimlaychuk on 10-Jul-17.
 */
public class Runner {
    private static NumberFormat nf = NumberFormat.getNumberInstance();
    private static long totalMessages = 0L;
    private static long totalSize = 0L;
    private static Configuration conf = new Configuration();
    private static Session session = Session.getInstance(conf, null);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        if (args.length == 0) {
            System.out.println("IMAP mailbox backup/restore util.");
            System.out.println("Edit build/resources/mbox.properties file before executing commands");
            System.out.println("NB! Folders with size = 0 are excluded from processing");
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
            System.out.println("... Not implemented");
        } else {
            System.out.println("Unknown option");
        }
        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " sec.");
        System.exit(0);
    }

    private static void backup() {
        try {
            Store store = session.getStore();
            store.connect(conf.getProperty("mailbox.user"), conf.getProperty("mailbox.password"));
            Folder folder = store.getDefaultFolder();

            backupFolder(folder, true);
            store.close();

            System.out.println("\nBackup messages: " + totalMessages);
            System.out.println("Backup size: " + nf.format(totalSize) + " bytes");

        } catch (MessagingException mex) {
            System.out.println(mex.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private static void check() {
        try {
            Store store = session.getStore();
            store.connect(conf.getProperty("mailbox.user"), conf.getProperty("mailbox.password"));
            Folder folder = store.getDefaultFolder();

            System.out.println("Reading folder structure...");
            dumpFolder(folder, true, "");
            store.close();
            System.out.println("\nTotal messages: " + totalMessages);
            System.out.println("Total size: " + nf.format(totalSize) + " bytes");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void backupFolder(Folder folder, boolean recurse) throws Exception {
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0 && folder.getMessageCount() > 0) {
            totalMessages += folder.getMessageCount();
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }
            backupMessages(folder);
            folder.close(false);
        }


        // Check if there are any subfolder to explore
        if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
            if (recurse) {
                Folder[] f = folder.list();
                for (Folder fld : f) backupFolder(fld, recurse);
            }
        }
    }

    private static void backupMessages(Folder folder) throws MessagingException, IOException {
        UIDFolder uf = (UIDFolder) folder; // cast folder to UIDFolder interface
        String folderPath = "backup/" + conf.getProperty("mailbox.domain") + "/" + conf.getProperty("mailbox.user") + "/" + folder.getFullName() + "/";
        Files.createDirectories(Paths.get(folderPath));
        System.out.println("Saving folder: " + folder.getFullName());

        // Create list of the existing files to remove them from synchronization
        List<String> existingFiles = Files.walk(Paths.get(folderPath))
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
                } else {
                    // skip existing file
                }

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            i++;
        }
        System.out.println();
        totalSize += size;
    }

    private static void dumpFolder(Folder folder, boolean recurse, String tab) throws Exception {
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
        if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
            if (recurse) {
                Folder[] f = folder.list();
                for (Folder fld : f) dumpFolder(fld, recurse, tab + "    ");
            }
        }
    }

    private static String getSize(Folder folder) throws MessagingException {
        UIDFolder uf = (UIDFolder) folder; // cast folder to UIDFolder interface
        Message[] messages = folder.getMessages();
        long size = 0L;
        for (Message msg : messages) {
            size = size + msg.getSize();
//            try {
//                //            Message m = new MimeMessage(session, new FileInputStream("1"));
//                //            System.out.println(m.getSubject());
//                File f = new File(String.valueOf(uf.getUID(msg)));
//                OutputStream os = new FileOutputStream(f);
//                msg.writeTo(os);
//                os.close();
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//            }
        }
        totalSize += size;
        return nf.format(size);
    }
}
