package se.lagrummet.rinfo.main

import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationMap
import org.apache.commons.configuration.ConfigurationException

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.store.depot.BeanUtilsURIConverter
import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.FileDepot

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.main.storage.CollectorLog
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler
import se.lagrummet.rinfo.main.storage.FeedCollectScheduler
import se.lagrummet.rinfo.main.storage.Storage
import se.lagrummet.rinfo.main.storage.StorageHandler
import se.lagrummet.rinfo.main.storage.StorageSession
import se.lagrummet.rinfo.main.storage.SourceFeedsConfigHandler


    // TODO: put inside Components (not possible in groovy 1.6).
    public static enum ConfigKey {
        DEPOT_BASE_URI("rinfo.depot.baseUri"),
        DEPOT_BASE_DIR("rinfo.depot.baseDir"),
        CONTAINER_DESCRIPTION_ENTRY_ID("rinfo.main.uriMinter.containerDescriptionEntryId"),
        SOURCE_FEEDS_ENTRY_ID("rinfo.main.storage.sourceFeedsEntryId"),
        ON_COMPLETE_PING_TARGETS("rinfo.main.collector.onCompletePingTargets"),
        PUBLIC_SUBSCRIPTION_FEED("rinfo.main.publicSubscriptionFeed"),
        COLLECTOR_LOG_DATA_DIR("rinfo.main.collector.logDataDir");

        private final String value;
        private ConfigKey(String value) { this.value = value; }
        public String toString() { return value; }
    }

/**
 * This is the Dependency Injection "hub" which is responsible for building the
 * components that constitute the application together. It will be the sole
 * entry point for all low-level configuration. The only exceptions are:
 * <ul>
 * <li>If a library uses hard-wired config locartions (e.g. logging).</li>
 * <li>If behaviour is configured from the domain (such as
 *     user/supplier-controlled data sources).</li>
 * </ul>
 */
public class Components {

    private Configuration config
    private Storage storage
    private FeedCollectScheduler collectScheduler

    static {
        BeanUtilsURIConverter.registerIfNoURIConverterIsRegistered()
    }

    public Components(Configuration config) {
        this.config = config
        checkConfig()
        setupStorage()
        setupCollectScheduler()
        setupStorageHandlers()
    }

    public Configuration getConfig() { return config }
    public Storage getStorage() { return storage }
    public FeedCollectScheduler getCollectScheduler() { return collectScheduler }

    protected void configure(Object bean, String subsetPrefix) {
        BeanUtils.populate(bean,
                new ConfigurationMap(config.subset(subsetPrefix)))
    }

    protected void checkConfig() throws ConfigurationException {
        List<String> missingKeys = new ArrayList<String>();
        for (ConfigKey cKey : ConfigKey.values()) {
            if (!config.containsKey(cKey.toString())) {
                missingKeys.add(cKey.toString());
            }
        }
        if (!missingKeys.isEmpty()) {
            throw new ConfigurationException("Configuration " + config +
                    " is missing the required keys: " + missingKeys.join(", "));
        }
    }

    protected void setupStorage() {
        storage = new Storage(createDepot(), createCollectorLog())
    }

    protected void setupCollectScheduler() {
        collectScheduler = new FeedCollectScheduler(storage)
        configure(collectScheduler, "rinfo.main.collector")
        def publicSubscriptionFeed = new URL(
                config.getString(ConfigKey.PUBLIC_SUBSCRIPTION_FEED.toString()))
        def onCompletePingTargets = new ArrayList<URL>()
        for (String s : config.getList(
                    ConfigKey.ON_COMPLETE_PING_TARGETS.toString())) {
            if (s == null || s.equals("")) continue
            onCompletePingTargets << new URL(s)
        }
        collectScheduler.setBatchCompletedCallback(
                new FeedUpdatePingNotifyer(publicSubscriptionFeed, onCompletePingTargets)
            )
    }

    protected void setupStorageHandlers() {
        storage.setStorageHandlers(createStorageHandlers())
    }

    private Depot createDepot() {
        Depot depot = new FileDepot()
        configure(depot, "rinfo.depot")
        /* TODO: the depot API should have atomizer less central; then:
        def atomizer = new AtomIndexer()
        configure(atomizer, "rinfo.main.atomizer")
        depot.setIndexer(atomizer)
        */
        //depot.initialize()
        return depot
    }

    private createCollectorLog() {
        return new CollectorLog(createRegistryRepo())
    }

    private Repository createRegistryRepo() {
        def dataDirPath = config.getString(ConfigKey.COLLECTOR_LOG_DATA_DIR.toString())
        def dataDir = new File(dataDirPath)
        if (!dataDir.exists()) {
            dataDir.mkdir()
        }
        return new SailRepository(new NativeStore(dataDir))
    }

    private Collection<StorageHandler> createStorageHandlers() {
        def storageHandlers = new ArrayList<StorageHandler>()
        storageHandlers.add(createSourceFeedsConfigHandler())
        //TODO: storageHandlers.add(createEntryRdfValidatorHandler())
        return storageHandlers
    }

    private StorageHandler createSourceFeedsConfigHandler() {
        URI sourceFeedsEntryId = new URI(config.getString(
                ConfigKey.SOURCE_FEEDS_ENTRY_ID.toString()))
        return new SourceFeedsConfigHandler(collectScheduler, sourceFeedsEntryId)
    }

    private StorageHandler createEntryRdfValidatorHandler() {
        // TODO: new URIMinter (takes less conf params.., data from depot..)
        def baseDir = config.getString("rinfo.main.baseDir")
        def repo = RDFUtil.slurpRdf(baseDir+"/datasets/containers.n3")
        def minterDir = baseDir+"/uri_algorithm"
        def uriMinter = new URIMinter(repo,
                minterDir+"/collect-uri-data.rq",
                minterDir+"/create-uri.xslt")
        URI containerEntryId = new URI(config.getString(
                ConfigKey.CONTAINER_DESCRIPTION_ENTRY_ID.toString()))
        return new EntryRdfValidatorHandler(uriMinter, containerEntryId)
    }

}
