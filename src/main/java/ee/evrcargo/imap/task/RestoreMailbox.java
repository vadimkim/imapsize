package ee.evrcargo.imap.task;

import ee.evrcargo.imap.Configuration;
import ee.evrcargo.imap.tree.FolderPath;
import ee.evrcargo.imap.tree.FolderState;
import ee.evrcargo.imap.tree.FolderTree;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class RestoreMailbox implements Task {
    private final Properties conf;
    private final int maxRetries;
    private Session session = null;
    private Store store = null;
    private char separator;

    public RestoreMailbox(String config) {
        conf = Configuration.getInstance().getProps(config);
        maxRetries = Integer.parseInt(conf.getProperty("mailbox.retry.count"));
    }

    @Override
    public void execute() {
        // Create IMAP session
        session = Session.getInstance(conf, null);
        try {
            store = session.getStore();
            store.connect(conf.getProperty("mailbox.user"), conf.getProperty("mailbox.password"));
            separator = store.getDefaultFolder().getSeparator();
        } catch (MessagingException e) {
            System.out.println(e.getMessage());
            System.out.println("Can't connect to IMAP server! Exiting...");
            return;
        }

        // Generate disk folder tree
        FolderTree tree = new FolderTree(conf);
        List<FolderPath> paths = tree.build();

        // Crawl the folders
        System.out.println("Restoring folders...");
        int retry = 0;
        int pathIdx = 0;
        FolderState folderState = new FolderState();
        List<String> imapIds = new ArrayList<>();
        while (retry < maxRetries && pathIdx < paths.size()) {
            if (restoreFolder(paths.get(pathIdx), folderState, imapIds)) {
                pathIdx++;
                folderState = new FolderState();
                imapIds = new ArrayList<>();
            } else {
                retry++;
                System.out.println("Failed message folder: " + paths.get(pathIdx).getPath() + " file number:" + folderState.getCurrentFile());
                System.out.println("Retry " + retry + "/" + maxRetries);
            }
        }

    }

    private boolean restoreFolder(FolderPath path, FolderState state, List<String> imapIds) {
        String diskFolder = conf.getProperty("mailbox.restore.base") + File.separator + path.getPath();
        InputStream is = null;
        try {
            // Create list of files at backup location
            List<String> existingFiles = Files.walk(Paths.get(diskFolder), 1)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .map(File::getName)
                    .collect(Collectors.toList());

            // Connect to remote folder
            System.out.println("Remote path:" + path.getPath());
            Folder remote = store.getFolder(path.getPath().replace(File.separator, String.valueOf(separator)));
            System.out.println("Reading remote folder: " + remote.getFullName() + "...");
            if (!remote.exists()) {
                System.out.println("Creating new folder at remote: " + remote.getFullName());
                remote.create(Folder.HOLDS_FOLDERS + Folder.HOLDS_MESSAGES);
            }
            remote.open(Folder.READ_WRITE);

            // Create list of message ID-s on the server
            Message[] messages = remote.getMessages();
            System.out.println("Getting server message id-s:");
            for (int i = state.getCurrentImap(); i < remote.getMessageCount(); i++) {
                System.out.print("Reading message " + i + "/" + messages.length + "\r");
                String[] id = messages[i].getHeader("Message-ID");
                if (id != null) {  //  if Message-ID exists use it as a key
                    imapIds.add(join(id));
                } else {            // if doesn't -- calculate MD5 of all header values and use it as a key
                    imapIds.add(headerMD5(messages[i].getAllHeaders()));
                }
                state.setCurrentImap(i);
            }

            // Restore messages to server
            int k = 0;
            for (int i = state.getCurrentFile(); i < existingFiles.size(); i++) {
                System.out.print("Restoring message " + (i + 1) + "/" + existingFiles.size() + "\r");
                is = new FileInputStream(diskFolder + "/" + existingFiles.get(i));
                Message msg = new MimeMessage(session, is);
                String[] id = msg.getHeader("Message-ID");
                String key;
                if (id != null) {
                    key = join(id);
                                    } else {
                    key = headerMD5(msg.getAllHeaders());
                }
                // Restore message only if key is missing
                if (!imapIds.contains(key)) {
                    remote.appendMessages(new Message[]{msg});
                    k++;
                }
                is.close();
                state.setCurrentFile(i);
            }
            System.out.println();
            System.out.println("... from which " + k + " are new");
            remote.close(false);

            return true;
        } catch (MessagingException | IOException | NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            try { if (is != null) is.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private static String headerMD5(Enumeration allHeaders) throws NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        StringBuilder allValues = new StringBuilder();
        while (allHeaders.hasMoreElements()) {
            Header header = (Header) allHeaders.nextElement();
            allValues.append(header.getValue());
        }

        byte[] hash = m.digest(allValues.toString().getBytes(Charset.forName("UTF-8")));
        return new String(Base64.getEncoder().encode(hash));
    }

    private static String join(String[] ids) {
        StringBuilder buf = new StringBuilder();
        for (String id : ids) {
            buf.append(id);
        }
        return buf.toString();
    }
}
