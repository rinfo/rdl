package se.lagrummet.rinfo.rdf.repo;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;


public class RepositoryHandlerFactory {

    public static final List<String> SUPPORTED_STORE_TYPES = Arrays.asList(
            "memory", "native" /*, "mysql", "pgsql", "mulgara", "virtuoso" */);

    /**
     * Create a new RepositoryHandler from the provided configuration.
     */
    public static final RepositoryHandler create(Configuration config) throws Exception {
        String repoId = config.getString("repositoryId");
        String inferenceType = config.getString("inferenceType");
        String storeType = config.getString("storeType").toLowerCase();

        if (!SUPPORTED_STORE_TYPES.contains(storeType)) {
            throw new ConfigurationException("Unsupported triple store: ["+storeType+"]");
        }

        String serverUrl = config.getString("remote.serverUrl");
        if (!StringUtils.isEmpty(serverUrl)) {
            return new RemoteRepositoryHandler(
                    repoId, storeType, inferenceType, serverUrl);
        }

        String dataDir = config.getString("native.dataDir");
        if (!StringUtils.isEmpty(dataDir)) {
            return new LocalRepositoryHandler(
                    repoId, storeType, inferenceType, dataDir);
        }

        throw new ConfigurationException(
                "Not enough properties to determine which RepositoryHandler to create.");
    }

}
