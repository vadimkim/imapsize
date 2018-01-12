package ee.evrcargo.imap.tree;

import ee.evrcargo.imap.Configuration;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import java.util.*;

import static javax.mail.Folder.HOLDS_FOLDERS;

public class ImapTree implements Tree {
    private Properties conf = Configuration.getInstance().getProps();
    private Store store;
    private List<FolderPath> mailboxMap = new ArrayList<>();

    @Override
    public List<FolderPath> build() {
        try {
            System.out.println("Reading IMAP folder structure...");
            Optional<Folder> initFolder = Optional.ofNullable(getDefaultFolder());
            if (initFolder.isPresent()) dumpFolder(initFolder.get(), 0);
            store.close();

        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
        return mailboxMap;
    }

    private void dumpFolder(Folder folder, int depth) throws MessagingException {
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0 && folder.getMessageCount() > 0) {
            FolderPath fp = new FolderPath(folder.getFullName(), depth);
            String tab = "  ";
            System.out.println(String.join("",Collections.nCopies(depth, tab)) + folder.getFullName());
            if (!isFiltered(fp.getPath())) mailboxMap.add(fp);  // Filter folders specified at configuration
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

    private boolean isFiltered (String name) {
        String[] patterns = conf.getProperty("mailbox.ignored.folders").split(",");
        for (String pattern : patterns) {
            if (name.equals(pattern.trim())) return true;
        }
        return false;
    }
}
