package ee.evrcargo.imap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Optional;
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
        FileInputStream input = null;
        try {
            Optional<URL> resource = Optional.ofNullable(Configuration.class.getResource("."));
            if (!resource.isPresent()) {
                input = new FileInputStream(new File("build/resources/main/mbox.properties"));
                this.props.load(new InputStreamReader(input, Charset.forName("UTF-8")));
                input.close();
            } else {
                // To run under IntelliJ  IDE
                String path = resource.get().getPath().split("out/production/")[0] + "out/production/resources/mbox.properties";
                input = new FileInputStream(new File(path));
                this.props.load(new InputStreamReader(input, Charset.forName("UTF-8")));
                input.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try { if (input != null) input.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public Properties getProps() {
        return props;
    }
}
