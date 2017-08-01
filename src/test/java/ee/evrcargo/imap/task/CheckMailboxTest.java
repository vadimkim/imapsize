package ee.evrcargo.imap.task;

import ee.evrcargo.imap.Configuration;
import ee.evrcargo.imap.tree.FolderPath;
import ee.evrcargo.imap.tree.ImapTree;
import org.junit.Test;
import org.junit.runner.RunWith;


import javax.mail.Session;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckMailboxTest {
    @Test
    public void execute() throws Exception {
        // Init objects
        List<FolderPath> paths = new ArrayList();
        paths.add(new FolderPath("INBOX", 1, 10));
        paths.add(new FolderPath("INBOX/Test", 2, 20));
        Session session = mock(Session.class);
        Configuration conf = mock(Configuration.class);
        when(conf.getProperty("mailbox.retry.count")).thenReturn("10");
        when(Session.getInstance(conf,null)).thenReturn(session);
        ImapTree tree = mock(ImapTree.class);
        when(tree.build()).thenReturn(paths);

        // Call method
        Task checkMailbox = new CheckMailbox();
        checkMailbox.execute();
        System.out.println("Done");
    }

}