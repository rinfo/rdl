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

import se.lagrummet.rinfo.rdf.repo.util.ConfigurationUtil;
import se.lagrummet.rinfo.rdf.repo.util.RepositoryRemover;


/**
 * Factory for local and remote (managed by a OpenRDF Sesame HTTP Server)
 * repositories.
 *
 * @author msher
 */
public class RepositoryFactory {

    /*
     *  NOTE: Local repositories are not handled by a RepositoryManager to
     *  enable what seems to be an easier way to connect to other triple stores
     *  using a SAIL connection, in contrast to the SailImplConfig used by a
     *  RepositoryManager. If possible though, a LocalRepositoryManager could
     *  be used for a more consistent implementation. /msher 081212
     */

    public static final List<String> SUPPORTED_TRIPLE_STORES = Arrays.asList(
            "sesame" /* "jena", "mulgara", "swiftowlim", "openlink-virtuoso" */);

    private static RemoteRepositoryManager remoteRepositoryManager;
    private static Repository remoteRepository;
    private static Repository localRepository;
    private static Configuration config;


    /**
     * Create RepositoryFactory with default configuration.
     */
    public RepositoryFactory() throws Exception {
        this(ConfigurationUtil.getDefaultConfiguration());
    }

    /**
     * Create RepositoryFactory with the provided configuration.
     */
    public RepositoryFactory(Configuration config) throws Exception {
        validateConfiguration(config);
        RepositoryFactory.config = config;
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
            remoteRepository = null;
        }
    }

    /**
     * Get local/remote repository according to configuration. Causes repository
     * to be initialised at first call, subsequent calls returns the same
     * repository.
     */
    public static synchronized Repository getRepository() throws Exception {
        if (config.getBoolean("use.local.repository")) {
            if (localRepository == null) {
                initLocalRepository();
            }
            return localRepository;
        } else {
            if (remoteRepository == null) {
                initRemoteRepository();
            }
            return remoteRepository;
        }
    }

    public static synchronized void removeLocalRepository() throws Exception {
        // TODO
        throw new NotImplementedException();
    }

    /**
     * Remove repository from the repository manager. All content in repository
     * is also removed.
     */
    public static synchronized void removeRemoteRepository() throws Exception {
        initRemoteRepository();
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
        String repoId = config.getString("repository.id");
        RepositoryRemover.dropRepository(repoId, remoteRepositoryManager);
    }

    /**
     * Create or retrieve an existing local repository with settings as provided
     * in the configuration.
     */
    private static void initLocalRepository() throws Exception {

        String store = config.getString("triple.store").toLowerCase();
        String backend = config.getString("backend").toLowerCase();
        String dataDir = config.getString("data.dir");
        String repoId = config.getString("repository.id");
        boolean inference = config.getBoolean("inference");
        boolean inferenceDT = config.getBoolean("inference.direct.type");

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
    private static void initRemoteRepository() throws Exception {

        String store = config.getString("triple.store").toLowerCase();
        String backend = config.getString("backend").toLowerCase();
        String repoId = config.getString("repository.id");
        String serverUrl = config.getString("remote.server.url");
        boolean inference = config.getBoolean("inference");
        boolean inferenceDT = config.getBoolean("inference.direct.type");

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

    /**
     * Check configuration for inconsistencies or missing properties.
     */
    private static void validateConfiguration(Configuration config) throws Exception {
        boolean useLocalRepository = config.getBoolean("use.local.repository");
        String store = config.getString("triple.store").toLowerCase();
        String backend = config.getString("backend").toLowerCase();
        String serverUrl = config.getString("remote.server.url");
        String dataDir = config.getString("data.dir");
        String repoId = config.getString("repository.id");
        boolean inference = config.getBoolean("inference");
        boolean inferenceDT = config.getBoolean("inference.direct.type");

        if (StringUtils.isEmpty(store)) {
            throw new Exception("Missing property 'store'.");
        }
        if (StringUtils.isEmpty(backend)) {
            throw new Exception("Missing property 'backend'.");
        }
        if (backend.equals("native") && StringUtils.isEmpty(dataDir)) {
            throw new Exception("Missing property 'data.dir'.");
        }
        if (backend.equals("native") && StringUtils.isEmpty(repoId)) {
            throw new Exception("Missing property 'repository.id'.");
        }
        if (!SUPPORTED_TRIPLE_STORES.contains(store)) {
            throw new Exception("Unsupported triple store: " + store);
        }
        if (!useLocalRepository && StringUtils.isEmpty(serverUrl)) {
            throw new Exception("Missing property 'remote.server.url'.");
        }
        if (inference && inferenceDT) {
            throw new Exception("Conflicting properties. 'inference' and "
                    + "'inference.direct.type' cannot both be true");
        }

    }
}
