package se.lagrummet.rinfo.rdf.repo.util;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Common default configuration loader.
 *
 * @author msher
 */
public class ConfigurationUtil {

    private static final String PROPERTIES_FILE_NAME = "rinfo-rdf-repo.properties";

    /**
     * Get properties from default configuration file.
     */
    public static Configuration getDefaultConfiguration()
    throws ConfigurationException {
        return new PropertiesConfiguration(PROPERTIES_FILE_NAME);
    }

}
