package se.lagrummet.rinfo.rdf.repo;

import org.apache.commons.lang.StringUtils;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.inferencer.fc.config.DirectTypeHierarchyInferencerConfig;
import org.openrdf.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;


/**
 * Handles a repository on a remote server.
 */
public class RemoteRepositoryHandler extends RepositoryHandler {

    private String serverUrl;
    private RemoteRepositoryManager remoteRepositoryManager;
    private Repository remoteRepository;

    public RemoteRepositoryHandler(String repoId, String storeType,
            String inferenceType, String serverUrl) throws Exception {
        super(repoId, storeType, inferenceType);
        this.serverUrl = serverUrl;
    }

    public Repository getRepository() {
        return remoteRepository;
    }

    /**
     * Create or retrieve an existing repository on a remote server.
     */
    public void initialize() throws Exception {
        if (remoteRepositoryManager == null) {
            remoteRepositoryManager = new RemoteRepositoryManager(serverUrl);
            remoteRepositoryManager.initialize();
        }

        remoteRepository = remoteRepositoryManager.getRepository(repoId);
        if (remoteRepository != null) {
            return;
        }

        SailImplConfig storeConfig = null;

        if (storeType.equals("memory")) {
            storeConfig = new MemoryStoreConfig(true); /* persist = true */
        } else if (storeType.equals("native")) {
            storeConfig = new NativeStoreConfig();
        } else {
            throw new Exception("Unsupported repository storeType: " + storeType);
        }

        if ("rdfs".equals(inferenceType)) {
            storeConfig = new ForwardChainingRDFSInferencerConfig(storeConfig);
        } else if ("dt".equals(inferenceType)) {
            storeConfig = new DirectTypeHierarchyInferencerConfig(storeConfig);
        }

        SailRepositoryConfig sailConfig = new SailRepositoryConfig(storeConfig);
        RepositoryConfig repoConfig = new RepositoryConfig(repoId, sailConfig);
        remoteRepositoryManager.addRepositoryConfig(repoConfig);
        remoteRepository = remoteRepositoryManager.getRepository(repoId);
    }

    /**
     * Remove repository from the repository manager. All content in repository
     * is also removed.
     */
    public synchronized void removeRepository() throws Exception {
        cleanRepository();
        RepositoryRemover.dropRepository(repoId, remoteRepositoryManager);
    }

    /**
     * Shuts down all initialised repositories and remote repository manager.
     */
    public void shutDown() throws RepositoryException {
        if (remoteRepositoryManager != null) {
            remoteRepositoryManager.shutDown();
            remoteRepositoryManager = null;
        }
        super.shutDown();
    }

}
