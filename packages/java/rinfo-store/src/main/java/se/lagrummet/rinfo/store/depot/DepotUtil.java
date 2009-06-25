package se.lagrummet.rinfo.store.depot;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.PropertiesConfiguration;


public class DepotUtil {

    public static final String DEFAULT_PROPERTIES_PATH = "rinfo-depot.properties";
    public static final String DEPOT_CONFIG_SUBSET_KEY = "rinfo.depot";

    static {
        BeanUtilsURIConverter.registerIfNoURIConverterIsRegistered();
    }

    public static Depot depotFromConfig() throws ConfigurationException {
        return depotFromConfig(DEFAULT_PROPERTIES_PATH, DEPOT_CONFIG_SUBSET_KEY);
    }

    public static Depot depotFromConfig(String propertiesPath)
            throws ConfigurationException {
        return depotFromConfig(propertiesPath, DEPOT_CONFIG_SUBSET_KEY);
    }

    public static Depot depotFromConfig(String propertiesPath, String subsetPrefix)
            throws ConfigurationException {
        Depot depot = new FileDepot();
        Configuration config = new PropertiesConfiguration(propertiesPath);
        if (subsetPrefix != null) {
            config = config.subset(subsetPrefix);
        }
        try {
            BeanUtils.populate(depot, new ConfigurationMap(config));
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
        return depot;
    }

}
