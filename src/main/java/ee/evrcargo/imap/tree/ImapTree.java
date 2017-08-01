package ee.evrcargo.imap.tree;

import ee.evrcargo.imap.Configuration;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static javax.mail.Folder.HOLDS_FOLDERS;

public class ImapTree implements Tree {
    private final String tab = "  ";
    private Configuration conf;
    private Store store;
    private List<FolderPath> mailboxMap = new ArrayList<>();

    public ImapTree(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public List<FolderPath> build() {
        try {
            System.out.println("Reading IMAP folder structure...");
            dumpFolder(getDefaultFolder(), 0);
            store.close();

        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
        return mailboxMap;
    }

    private void dumpFolder(Folder folder, int depth) throws MessagingException {
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0 && folder.getMessageCount() > 0) {
            FolderPath fp = new FolderPath(folder.getFullName(), depth, folder.getMessageCount());
            System.out.println(String.join("",Collections.nCopies(depth, tab)) + folder.getFullName());
            if (!folder.getFullName().contains("Calendar") && !folder.getFullName().contains("Contacts")) mailboxMap.add(fp);  // Filter "Calendar"  and "Contact" folder
        }

        // Check if there are any subfolder to explore
        if ((folder.getType() & HOLDS_FOLDERS) != 0) {
            Folder[] f = folder.list();
            for (Folder fld : f) dumpFolder(fld, depth + 1);
        }
    }

    private Folder getDefaultFolder() {
        try {
            Session session = Session.getInstance(conf, null);
            store = session.getStore();
            store.connect(conf.getProperty("mailbox.user"), conf.getProperty("mailbox.password"));
            return store.getDefaultFolder();
        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
