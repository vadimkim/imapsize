package ee.evrcargo.imap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;

/**
 * Created by vadim.kimlaychuk on 12-Jul-17.
 */
public class Configuration {

    private HashMap<String, Properties> props = new HashMap<String, Properties>();

    private static final Configuration instance = new Configuration();

    public static Configuration getInstance() {
        return instance;
    }

    private Configuration() {
    }

    /**
     * @param filename mbox.properties file to load, or <code>null</code> to load the default file
     * @return
     */
    public Properties getProps(String filename) {
        if (this.props.containsKey(filename))
            return this.props.get(filename);

        Properties props = new Properties();
        try {
            if (filename != null) {
                try (FileInputStream input = new FileInputStream(new File(filename))) {
                    props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                }
            } else {
                Optional<URL> resource = Optional.ofNullable(Configuration.class.getResource("."));
                if (!resource.isPresent()) {
                    try (FileInputStream input = new FileInputStream(new File("build/resources/main/mbox.properties"))) {
                        props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                    }
                } else {
                    // To run under IntelliJ  IDE
                    String path = resource.get().getPath().split("out/production/")[0] + "out/production/resources/mbox.properties";
                    try (FileInputStream input = new FileInputStream(new File(path))) {
                        props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        this.props.put(filename, props);

        return props;
    }
}
