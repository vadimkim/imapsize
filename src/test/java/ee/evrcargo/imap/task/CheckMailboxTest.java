package ee.evrcargo.imap.task;

import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

public class CheckMailboxTest {
    @Test
    public void execute() {
        // Init objects
        Properties conf  = new Properties();
        conf.setProperty("mail.host", "outlook.office365.com");
        conf.setProperty("mail.store.protocol", "imaps");
        conf.setProperty("mail.imap.partialfetch", "false");
        conf.setProperty("mail.debug", "true");
        conf.setProperty("mailbox.user", "");
        conf.setProperty("mailbox.password", "");
        try {
            Session session = Session.getInstance(conf, null);
            Store store = session.getStore();
            store.connect(conf.getProperty("mailbox.user"), conf.getProperty("mailbox.password"));
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            Message msg = new MimeMessage(session, new FileInputStream("111"));
            folder.appendMessages(new Message[] {msg});

        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

}