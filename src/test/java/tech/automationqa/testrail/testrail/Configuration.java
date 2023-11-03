package tech.automationqa.testrail.testrail;

import com.intuit.karate.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Provides utility methods to retrieve configuration values from a Yaml configuration file.
 * Supports different profiles and default configurations.
 */
@SuppressWarnings("ALL")
public class Configuration {

    private static final Logger logger = new Logger();
    private static final String DEFAULT_PROFILE = "Default";
    private static Optional<Map<String, Object>> defaultMap = Optional.empty();
    private static Optional<Map<String, Object>> profileMap = Optional.empty();

    static {
        Yaml yaml = new Yaml();
        try {
            Optional<Map<String, Object>> configMap = Optional.of(yaml.load(Configuration.class.getResource("/testrail-config.yml").openStream()));
            defaultMap = Optional.of((Map<String, Object>) configMap.get().get(DEFAULT_PROFILE));
            if (System.getProperty("environment") != null) {
                String activeProfile = System.getProperty("environment");
                profileMap = Optional.of((Map<String, Object>) configMap.get().get(activeProfile));
            }
        } catch (IOException e) {
            logger.info("Configuration file is missing");
        }
    }


    private static Optional<Object> fetchConfigurationProperty(String key) {
        return profileMap
                .map(map -> map.get(key))
                .or(() -> defaultMap.map(map -> map.get(key)));
    }


    /**
     * Retrieves a configuration property as a String.
     *
     * @param key The key of the configuration property.
     * @return An Optional containing the configuration property value, if found.
     */
    public static Optional<String> getConfigurationString(String key) {
        return fetchConfigurationProperty(key).map(String::valueOf);
    }

    /**
     * Retrieves a configuration property as a Boolean.
     *
     * @param key The key of the configuration property.
     * @return An Optional containing the configuration property value, if found.
     */
    public static Optional<Boolean> getConfigurationBoolean(String key) {
        return fetchConfigurationProperty(key).map(obj -> Boolean.parseBoolean(obj.toString()));
    }

    /**
     * Retrieves a configuration property as an Integer.
     *
     * @param key The key of the configuration property.
     * @return An Optional containing the configuration property value, if found.
     */
    public static Optional<Integer> getConfigurationInteger(String key) {
        return fetchConfigurationProperty(key).map(obj ->Integer.valueOf(obj.toString()));
    }

}