package ee.evrcargo.imap.task;

import ee.evrcargo.imap.Configuration;
import ee.evrcargo.imap.tree.FolderPath;
import ee.evrcargo.imap.tree.FolderState;
import ee.evrcargo.imap.tree.ImapTree;

import javax.mail.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class BackupMailbox implements Task {
    private static NumberFormat nf = NumberFormat.getNumberInstance();
    private final Properties conf;
    private final int maxRetries;

    public BackupMailbox(String config) {
        conf = Configuration.getInstance().getProps(config);
        maxRetries = Integer.parseInt(conf.getProperty("mailbox.retry.count"));
    }

    @Override
    public void execute() {
        System.out.println("Executing backup for mailbox " + conf.getProperty("mailbox.user") + " to backup/" + conf.getProperty("mailbox.domain") + "/" + conf.getProperty("mailbox.user"));
        ImapTree tree = new ImapTree(conf);
        List<FolderPath> paths = tree.build();

        // Crawl the folders
        System.out.println("\n                  Backing up the folders... \n");
        int retry = 0;
        int pathIdx = 0;
        FolderState folderState = new FolderState();
        List<FolderState> allStates = new ArrayList<>();
        while (retry < maxRetries && pathIdx < paths.size()) {
            if (backupFolder(paths.get(pathIdx), folderState)) {
                allStates.add(folderState);
                pathIdx++;
                folderState = new FolderState();
            } else {
                retry++;
                System.out.println("Retry " + retry + "/" + maxRetries);
            }
        }

        // Print totals
        int messageTotal = 0;
        long sizeTotal = 0L;
        for (FolderState state : allStates) {
            messageTotal += state.getCurrentImap();
            sizeTotal += state.getSize();
        }

        System.out.println("\nNB! Filtered folders are: " + conf.getProperty("mailbox.ignored.folders"));
        System.out.println("Total messages: " + messageTotal);
        System.out.println("Mailbox size: " + nf.format(sizeTotal) + " bytes");
    }

    private boolean backupFolder(FolderPath path, FolderState state) {
        Session session = Session.getInstance(conf, null);
        Store store = null;
        try {
            store = session.getStore();
            store.connect(conf.getProperty("mailbox.user"), conf.getProperty("mailbox.password"));
            Folder folder = store.getFolder(path.getPath());
            UIDFolder uf = (UIDFolder) folder; // cast folder to UIDFolder interface
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }

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
            for (int i = state.getCurrentImap(); i < folder.getMessageCount(); i++) {
                System.out.print("Saving message " + (i + 1) + "/" + folder.getMessageCount() + "\r");
                state.setSize(state.getSize() + messages[i].getSize());
                state.setCurrentImap(i);
                if (!existingFiles.contains(String.valueOf(uf.getUID(messages[i])))) {
                    File f = new File(folderPath + String.valueOf(uf.getUID(messages[i])));
                    OutputStream os = new FileOutputStream(f);
                    messages[i].writeTo(os);
                    os.close();
                }
            }
            System.out.println();

            store.close();
            return true;
        } catch (MessagingException | IOException e) {
            System.out.println(e.getMessage());
            System.out.println("\nFailed message folder: " + path.getPath() + " number:" + state.getCurrentImap());
            return false;
        } finally {
            try { if (store != null) store.close(); } catch (MessagingException e) { e.printStackTrace(); }
        }
    }
}
