package ch.bzz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads the local configuration file. Credentials are never hardcoded, so
 * {@code config.properties} only exists locally and is not checked into Git.
 */
public final class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = load();

    private Config() {
        // utility class
    }

    /**
     * @return a copy of all configured properties, e.g. to be passed to the EntityManagerFactory
     */
    public static Properties getProperties() {
        Properties copy = new Properties();
        copy.putAll(PROPERTIES);
        return copy;
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    private static Properties load() {
        Properties properties = new Properties();

        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            log.info("Configuration loaded from {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Could not read the configuration file {}. "
                    + "Create it based on config.properties.template.", CONFIG_FILE, e);
        }

        return properties;
    }
}
