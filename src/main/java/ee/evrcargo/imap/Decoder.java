package ee.evrcargo.imap;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BASE64MailboxDecoder;

/**
 * Created by vadim.kimlaychuk on 11-Jul-17.
 */
public class Decoder {

    public static void main (String[] args) {
        String name = "&BCcENQRABD0EPgQyBDgEOgQ4-";
        System.out.println (BASE64MailboxDecoder.decode(name));
    }
}
