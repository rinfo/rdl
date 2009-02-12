package se.lagrummet.rinfo.rdf.repo;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.Sail;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.inferencer.fc.config.DirectTypeHierarchyInferencerConfig;
import org.openrdf.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.memory.config.MemoryStoreConfig;
import org.openrdf.sail.nativerdf.NativeStore;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;

import se.lagrummet.rinfo.rdf.repo.util.RepositoryRemover;


// TODO: split this into subclasses for local and remote (and other future stores).
// TODO: always call initialize? (see "setup" of the cmdline-tool..)

/**
 * Handler for local and remote (managed by a OpenRDF Sesame HTTP Server)
 * repositories.
 */
public class RepositoryHandler {

    /*
     *  NOTE: Local repositories are not handled by a RepositoryManager to
     *  enable what seems to be an easier way to connect to other triple stores
     *  using a SAIL connection, in contrast to the SailImplConfig used by a
     *  RepositoryManager. If possible though, a LocalRepositoryManager could
     *  be used for a more consistent implementation. /msher 081212
     */

    public static final List<String> SUPPORTED_TRIPLE_STORES = Arrays.asList(
            "sesame" /* "jena", "mulgara", "swiftowlim", "openlink-virtuoso" */);

    private RemoteRepositoryManager remoteRepositoryManager;
    private Repository remoteRepository;
    private Repository localRepository;

    private String store;
    private String backend;
    private boolean useLocalRepository;
    private String serverUrl;
    private String dataDir;
    private String repoId;
    private boolean inference;
    private boolean inferenceDT;


    /**
     * Create RepositoryHandler with the provided configuration.
     */
    public RepositoryHandler(Configuration config) throws Exception {
        this(
                config.getString("triple.store").toLowerCase(),
                config.getString("backend").toLowerCase(),
                config.getBoolean("use.local.repository"),
                config.getString("remote.server.url"),
                config.getString("data.dir"),
                config.getString("repository.id"),
                config.getBoolean("inference"),
                config.getBoolean("inference.direct.type")
            );
    }

    public RepositoryHandler(String store, String backend,
            boolean useLocalRepository, String serverUrl, String dataDir,
            String repoId,
            boolean inference, boolean inferenceDT) throws Exception {
        this.store = store;
        this.backend = backend;
        this.useLocalRepository = useLocalRepository;
        this.serverUrl = serverUrl;
        this.dataDir = dataDir;
        this.repoId = repoId;
        this.inference = inference;
        this.inferenceDT = inferenceDT;
        validate();
    }

    /**
     * Validate the setup (checks for inconsistencies or missing values).
     */
    private void validate() throws IllegalStateException {
        if (StringUtils.isEmpty(store)) {
            throw new IllegalStateException("Missing property 'store'.");
        }
        if (StringUtils.isEmpty(backend)) {
            throw new IllegalStateException("Missing property 'backend'.");
        }
        if (backend.equals("native") && StringUtils.isEmpty(dataDir)) {
            throw new IllegalStateException("Missing property 'data.dir'.");
        }
        if (backend.equals("native") && StringUtils.isEmpty(repoId)) {
            throw new IllegalStateException("Missing property 'repository.id'.");
        }
        if (!SUPPORTED_TRIPLE_STORES.contains(store)) {
            throw new IllegalStateException("Unsupported triple store: " + store);
        }
        if (!useLocalRepository && StringUtils.isEmpty(serverUrl)) {
            throw new IllegalStateException("Missing property 'remote.server.url'.");
        }
        if (inference && inferenceDT) {
            throw new IllegalStateException("Conflicting properties. 'inference' and "
                    + "'inference.direct.type' cannot both be true");
        }
    }

    public Repository getRepository() {
        return useLocalRepository? localRepository : remoteRepository;
    }

    /**
     * Get local/remote repository according to configuration. Causes repository
     * to be initialised at first call, subsequent calls returns the same
     * repository.
     */
    public synchronized void initialize() throws Exception {
        if (useLocalRepository) {
            if (localRepository == null) {
                initLocalRepository();
            }
        } else {
            if (remoteRepository == null) {
                initRemoteRepository();
            }
        }
    }

    public synchronized void cleanRepository() throws Exception {
        Repository repo = useLocalRepository? localRepository : remoteRepository;
        RepositoryConnection conn = repo.getConnection();
        conn.clear();
        conn.clearNamespaces();
        conn.close();
    }

    public synchronized void removeRepository() throws Exception {
        if (localRepository != null) {
            removeLocalRepository();
        }
        if (remoteRepository != null) {
            removeRemoteRepository();
        }
    }

    /**
     * Shuts down all initialised repositories and remote repository manager.
     */
    public void shutDown() throws RepositoryException {
        if (localRepository != null) {
            localRepository.shutDown();
            localRepository = null;
        }
        if (remoteRepositoryManager != null) {
            remoteRepositoryManager.shutDown();
            remoteRepositoryManager = null;
            remoteRepository.shutDown();
            remoteRepository = null;
        }
    }


    /**
     * Create or retrieve an existing local repository with settings as provided
     * in the configuration.
     */
    private void initLocalRepository() throws Exception {

        if (store.equals("sesame")) {

            Sail sail = null;
            if (backend.equals("memory")) {
                sail = new MemoryStore();
            } else if (backend.equals("native")) {
                sail = new NativeStore(new File(dataDir + "/" + repoId));
            }

            if (inference) {
                localRepository = new SailRepository(
                        new ForwardChainingRDFSInferencer(sail));
            } else if (inferenceDT) {
                localRepository = new SailRepository(
                        new DirectTypeHierarchyInferencer(sail));
            } else {
                localRepository = new SailRepository(sail);
            }

            localRepository.initialize();

        } else {
            throw new Exception("Unsupported repository type: " + store);
        }
    }

    /**
     * Create or retrieve an existing repository on a remote server with
     * settings as provided in the configuration.
     */
    private void initRemoteRepository() throws Exception {

        if (remoteRepositoryManager == null) {
            remoteRepositoryManager = new RemoteRepositoryManager(serverUrl);
            remoteRepositoryManager.initialize();
        }

        remoteRepository = remoteRepositoryManager.getRepository(repoId);

        if (remoteRepository != null) {
            return;
        }

        // create new repository
        if (store.equals("sesame")) {

            SailImplConfig storeConfig = null;

            if (backend.equals("memory")) {
                storeConfig = new MemoryStoreConfig(true); /* persist = true */
            } else if (backend.equals("native")) {
                storeConfig = new NativeStoreConfig();
            } else {
                throw new Exception("Unsupported repository backend: " + backend);
            }

            if (inference) {
                storeConfig = new ForwardChainingRDFSInferencerConfig(storeConfig);
            } else if (inferenceDT) {
                storeConfig = new DirectTypeHierarchyInferencerConfig(storeConfig);
            }

            SailRepositoryConfig sailConfig = new SailRepositoryConfig(storeConfig);
            RepositoryConfig repoConfig = new RepositoryConfig(repoId, sailConfig);
            remoteRepositoryManager.addRepositoryConfig(repoConfig);
            remoteRepository = remoteRepositoryManager.getRepository(repoId);

        } else {
            throw new Exception("Unsupported repository type: " + store);
        }

    }

    private synchronized void removeLocalRepository() throws Exception {
        // TODO
        throw new NotImplementedException(
                "Error: 'remove' is not implemented for local repository");
    }

    /**
     * Remove repository from the repository manager. All content in repository
     * is also removed.
     */
    private synchronized void removeRemoteRepository() throws Exception {
        RepositoryConnection conn = null;
        try {
            if (remoteRepository != null) {
                conn = remoteRepository.getConnection();
                conn.clear();
                conn.clearNamespaces();
            }
        } finally {
            if (conn != null && conn.isOpen()) {
                conn.close();
            }
        }
        RepositoryRemover.dropRepository(repoId, remoteRepositoryManager);
    }

}
