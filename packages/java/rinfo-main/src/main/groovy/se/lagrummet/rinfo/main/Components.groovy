package se.lagrummet.rinfo.main

import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationMap
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.lang.StringUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.store.depot.BeanUtilsURIConverter
import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.LockedDepotEntryException

import se.lagrummet.rinfo.collector.atom.CompleteFeedEntryIdIndex
import se.lagrummet.rinfo.collector.atom.fs.CompleteFeedEntryIdIndexFSImpl

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.main.storage.CollectorLog
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler
import se.lagrummet.rinfo.main.storage.FeedCollectScheduler
import se.lagrummet.rinfo.main.storage.FeedCollector
import se.lagrummet.rinfo.main.storage.Storage
import se.lagrummet.rinfo.main.storage.StorageHandler
import se.lagrummet.rinfo.main.storage.SourceFeedsConfigHandler


/**
 * This is the dependency injection hub, creating and sewing together all
 * components of the application. It is the sole entry point for all low-level
 * configuration. The only exceptions are:
 * <ul>
 * <li>If a library uses hard-wired config locartions (e.g. logging).</li>
 * <li>If behaviour is configured from the domain (such as collected (external)
 *     data sources).</li>
 * </ul>
 */
public class Components {

    public static enum ConfigKey {
        DEPOT_BASE_URI("rinfo.depot.baseUri"),
        DEPOT_BASE_DIR("rinfo.depot.baseDir"),
        SOURCE_FEEDS_ENTRY_ID("rinfo.main.storage.sourceFeedsEntryId"),
        RDF_CHECK_BASE_PATH("rinfo.main.rdfcheck.basePath"),
        RDF_CHECKER_VOCAB_ENTRY_IDS("rinfo.main.checker.vocabEntryIds"),
        URIMINTER_ENTRY_ID("rinfo.main.uriMinter.uriSpaceEntryId"),
        URIMINTER_URI_SPACE_URI("rinfo.main.uriMinter.uriSpaceUri"),
        ON_COMPLETE_PING_TARGETS("rinfo.main.collector.onCompletePingTargets", false),
        PUBLIC_SUBSCRIPTION_FEED("rinfo.main.publicSubscriptionFeed"),
        COLLECTOR_LOG_DATA_DIR("rinfo.main.collector.logDataDir"),
        COMPLETE_FEEDS_ID_INDEX_DIR("rinfo.main.collector.completeFeedsIndexDir"),
        SYSTEM_BASE_URI("rinfo.main.collectorLog.systemBaseUri"),
        ENTRY_DATASET_URI("rinfo.main.collectorLog.entryDatasetUri");

        public final String value;
        public final boolean requiredValue = false;
        private ConfigKey(String value) { this.value = value; }
        private ConfigKey(String value, boolean requiredValue) {
            this.value = value;
            this.requiredValue = requiredValue;
        }
        public String toString() { return value; }
    }

    private Configuration config
    private Storage storage
    private FeedCollector feedCollector
    private FeedCollectScheduler collectScheduler
    private CollectorLog collectorLog

    static {
        BeanUtilsURIConverter.registerIfNoURIConverterIsRegistered()
    }

    private final Logger logger = LoggerFactory.getLogger(Components.class)

    public Components(Configuration config) {
        this.config = config
        checkConfig()
        setupCollectorLog()
        setupStorage()
        setupFeedCollector()
        setupCollectScheduler()
        setupStorageHandlers()
    }

    public Configuration getConfig() { return config; }
    public Storage getStorage() { return storage; }
    public CollectorLog getCollectorLog() { return collectorLog; }
    public FeedCollectScheduler getCollectScheduler() { return collectScheduler; }
    public FeedCollector getFeedCollector() { return feedCollector; }

    public void startup() {
        storage.startup()
        collectScheduler.startup()
    }
    public void shutdown() {
        try {
            collectScheduler.shutdown()
        } finally {
            storage.shutdown()
        }
    }
    protected void configure(Object bean, String subsetPrefix) {
        BeanUtils.populate(bean,
                new ConfigurationMap(config.subset(subsetPrefix)))
    }

    protected void checkConfig() throws ConfigurationException {
        List<String> missingKeys = new ArrayList<String>();
        for (ConfigKey cKey : ConfigKey.values()) {
            if (!config.containsKey(cKey.toString()) ||
                cKey.requiredValue && StringUtils.isEmpty(config.getString(cKey.toString()))) {
                missingKeys.add(cKey.toString());
            }
        }
        if (!missingKeys.isEmpty()) {
            throw new ConfigurationException("Configuration " + config +
                    " is missing the required keys: " + missingKeys.join(", "));
        }
    }

    private void setupCollectorLog() {
        collectorLog = new CollectorLog(createRegistryRepo())
        configure(collectorLog, "rinfo.main.collectorLog")
    }

    protected void setupStorage() {
        storage = new Storage(createDepot(), collectorLog,
                createCompleteFeedEntryIdIndex())
    }

    protected void setupFeedCollector() {
        feedCollector = new FeedCollector(storage)
    }

    protected void setupCollectScheduler() {
        collectScheduler = new FeedCollectScheduler(feedCollector)
        configure(collectScheduler, "rinfo.main.collector")
        def publicSubscriptionFeed = new URL(
                config.getString(ConfigKey.PUBLIC_SUBSCRIPTION_FEED.value))
        def onCompletePingTargets = new ArrayList<URL>()
        for (String s : config.getList(
                    ConfigKey.ON_COMPLETE_PING_TARGETS.value)) {
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
        depot.initialize()

        boolean checkDepot = true
        if (checkDepot) {
            // TODO:IMPROVE: This unconditionally expects that the
            // locked entry has not been indexed (which it should not have
            // been, if lock occurred during collect). But how about:
            //if (!e.lockedEntry.isIndexed()) { ... }

            def start = new Date().time
            logger.info("Checking depot consistency..")
            for (lockedEntry in depot.iterateLockedEntries()) {
                logger.info("Found locked depot entry: <${lockedEntry}>. " +
                        "Rolling it back.")
                lockedEntry.rollback()
            }
            logger.info("Done in ${new Date().time - start} ms.")

            // TODO:?
            //atomizer.checkFeedChain()
            //(depot & atomizer).ensureIndexedEnties()
        }
        return depot
    }

    private CompleteFeedEntryIdIndex createCompleteFeedEntryIdIndex() {
        def completeFeedsIdIndexDir = new File(config.getString(
                ConfigKey.COMPLETE_FEEDS_ID_INDEX_DIR.value))
        ensureDir(completeFeedsIdIndexDir)
        return new CompleteFeedEntryIdIndexFSImpl(completeFeedsIdIndexDir);
    }

    private Repository createRegistryRepo() {
        def dataDirPath = config.getString(ConfigKey.COLLECTOR_LOG_DATA_DIR.value)
        def dataDir = new File(dataDirPath)
        ensureDir(dataDir)
        def repo = new SailRepository(new NativeStore(dataDir))
        repo.initialize()
        return repo
    }

    private Collection<StorageHandler> createStorageHandlers() {
        def storageHandlers = new ArrayList<StorageHandler>()
        storageHandlers.add(createSourceFeedsConfigHandler())
        //TODO: storageHandlers.add(createEntryRdfValidatorHandler())
        return storageHandlers
    }

    private StorageHandler createSourceFeedsConfigHandler() {
        return new SourceFeedsConfigHandler(
                collectScheduler,
                config.getString(ConfigKey.SOURCE_FEEDS_ENTRY_ID.value))
    }

    private StorageHandler createEntryRdfValidatorHandler() {
        return new EntryRdfValidatorHandler(
                config.getString(ConfigKey.RDF_CHECK_BASE_PATH.value),
                config.getList(ConfigKey.RDF_CHECKER_VOCAB_ENTRY_IDS.value),
                config.getString(ConfigKey.URIMINTER_ENTRY_ID.value),
                config.getString(ConfigKey.URIMINTER_URI_SPACE_URI.value))
    }

    private void ensureDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Cannot create directory: " + dir);
            }
        }
    }
}
