package ee.evrcargo.imap;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Created by vadim.kimlaychuk on 12-Jul-17.
 */
public class Configuration extends Properties {

    public Configuration() {
        super();
        try {
            URL resource = Configuration.class.getResource(".");
            if (resource == null) {
                this.load(new FileInputStream("build/resources/main/mbox.properties"));
            } else {
                String path = resource.getPath().split("out/production/")[0] + "out/production/resources/mbox.properties";
                this.load(new FileInputStream(path));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
