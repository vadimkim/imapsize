package ee.evrcargo.imap;

import ee.evrcargo.imap.task.CheckMailbox;
import ee.evrcargo.imap.task.RestoreMailbox;
import ee.evrcargo.imap.task.Task;

import javax.mail.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
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
    private static Task task;

    public static void main(String[] args) throws MessagingException {
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
            task = new CheckMailbox();
        } else if (args[0].compareTo("-r") == 0) {
            task = new RestoreMailbox();
        } else {
            System.out.println("Unknown option");
            System.exit(-1);
        }
        task.execute();
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

    private static void backupFolder(Folder folder) throws MessagingException, IOException {
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
     * @throws MessagingException -- IMAP exception
     * @throws IOException        -- file exception
     */
    private static void backupMessages(Folder folder) throws IOException, MessagingException {
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
            try {
                size = size + msg.getSize();
                System.out.print("Saving message " + i + "/" + folder.getMessageCount() + "\r");
                if (!existingFiles.contains(String.valueOf(uf.getUID(msg)))) {
                    File f = new File(folderPath + String.valueOf(uf.getUID(msg)));
                    OutputStream os = new FileOutputStream(f);
                    msg.writeTo(os);
                    os.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } catch (MessagingException e) {
                System.out.println(e.getMessage());
            }
            i++;
        }
        System.out.println();
        totalSize += size;
    }
}
