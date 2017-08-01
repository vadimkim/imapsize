package ee.evrcargo.imap.task;

import javax.mail.MessagingException;

public interface Task {
    void execute() throws MessagingException;
}
