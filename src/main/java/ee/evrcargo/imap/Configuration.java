package ee.evrcargo.imap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Created by vadim.kimlaychuk on 12-Jul-17.
 */
public class Configuration {

    private Properties props = new Properties();

    private static final Configuration instance = new Configuration();

    public static Configuration getInstance() {
        return instance;
    }

    private Configuration() {
        super();
        try {
            URL resource = Configuration.class.getResource(".");
            FileInputStream input;
            if (resource == null) {
                input = new FileInputStream(new File("build/resources/main/mbox.properties"));
            } else {
                String path = resource.getPath().split("out/production/")[0] + "out/production/resources/mbox.properties";
                input = new FileInputStream(new File(path));
            }
            this.props.load(new InputStreamReader(input, Charset.forName("UTF-8")));
            input.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public Properties getProps() {
        return props;
    }
}
