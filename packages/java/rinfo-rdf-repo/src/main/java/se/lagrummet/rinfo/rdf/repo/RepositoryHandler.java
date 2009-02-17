package se.lagrummet.rinfo.rdf.repo;

import org.apache.commons.lang.StringUtils;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;


/**
 * Handler for local and remote (managed by a OpenRDF Sesame HTTP Server)
 * repositories.
 */
public abstract class RepositoryHandler {

    String repoId;
    String storeType;
    String inferenceType;

    public RepositoryHandler(String repoId, String storeType,
            String inferenceType) throws Exception {

        if (StringUtils.isEmpty(storeType)) {
            throw new Exception("Missing property 'storeType'.");
        }
        if (storeType.equals("native") && StringUtils.isEmpty(repoId)) {
            throw new Exception("Missing property 'repositoryId'.");
        }

        this.repoId = repoId;
        this.storeType = storeType;
        this.inferenceType = inferenceType;
    }

    public abstract Repository getRepository();

    /**
     * Initialize (create or retrieve) a repository according to configuration.
     */
    public abstract void initialize() throws Exception;

    public abstract void removeRepository() throws Exception;

    public synchronized void cleanRepository() throws Exception {
        RepositoryConnection conn = getRepository().getConnection();
        try {
            conn.clear();
            conn.clearNamespaces();
        } finally {
            if (conn.isOpen()) {
                conn.close();
            }
        }
    }

    public void shutDown() throws RepositoryException {
        getRepository().shutDown();
    }

}
