package se.lagrummet.rinfo.rdf.repo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;


public class RepositoryHandlerFactory {

    /**
     * Create a new RepositoryHandler from the provided configuration.
     */
    public static final RepositoryHandler create(Configuration config) throws Exception {
        String repoId = config.getString("repositoryId");
        String inferenceType = config.getString("inferenceType");
        String storeType = config.getString("storeType").toLowerCase();

        String serverUrl = config.getString("remote.serverUrl");
        if (!StringUtils.isEmpty(serverUrl)) {
            return new RemoteRepositoryHandler(serverUrl, repoId,
                    storeType, inferenceType);
        }

        String dataDir = config.getString("native.dataDir");
        if (!StringUtils.isEmpty(dataDir)) {
            return new LocalRepositoryHandler(dataDir, repoId,
                    storeType, inferenceType);
        }

        throw new ConfigurationException(
                "Not enough properties to determine which RepositoryHandler to create.");
    }

}
