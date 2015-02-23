package se.lagrummet.rinfo.main

//import groovy.transform.CompileStatic

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
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.LockedDepotEntryException

import se.lagrummet.rinfo.collector.atom.FeedEntryDataIndex
import se.lagrummet.rinfo.collector.atom.fs.FeedEntryDataIndexFSImpl

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
 * This is the dependency injection hub, creating and stitching together all
 * components of the application. It is the sole entry point for all low-level
 * configuration. The only exceptions are:
 * <ul>
 * <li>If a library uses hard-wired config locations (e.g. logging).</li>
 * <li>If behaviour is configured from the domain (such as collected (external)
 *     data sources).</li>
 * </ul>
 */
//@CompileStatic // TODO: enable when greclipse 2.7.1 is released
class Components {

    static enum ConfigKey {
        DEPOT_BASE_URI("rinfo.depot.baseUri"),
        DEPOT_BASE_DIR("rinfo.depot.baseDir"),
        SOURCE_FEEDS_ENTRY_ID("rinfo.main.storage.sourceFeedsEntryId"),
        SYSTEM_DATASET_URI("rinfo.main.storage.datasetUri"),
        REPORT_BASE_URI("rinfo.main.storage.reportBaseUri"),
        CHECKER_CHECKED_BASE_PATH("rinfo.main.checker.checkedBasePath"),
        CHECKER_VOCAB_ENTRY_IDS("rinfo.main.checker.vocabEntryIds"),
        VALIDATION_ENTRY_ID("rinfo.main.checker.validationEntryId"),
        URIMINTER_ENTRY_ID("rinfo.main.uriMinter.uriSpaceEntryId"),
        URIMINTER_URI_SPACE_URI("rinfo.main.uriMinter.uriSpaceUri"),
        COLLECTOR_HTTP_TIMEOUT_SECONDS("rinfo.main.collector.http.timeoutSeconds"),
        COLLECTOR_HTTP_ALLOW_SELF_SIGNED("rinfo.main.collector.http.allowSelfSigned"),
        ADMIN_FEED_ID("rinfo.main.collector.adminFeedId"),
        ADMIN_FEED_URL("rinfo.main.collector.adminFeedUrl"),
        ON_COMPLETE_PING_TARGETS("rinfo.main.collector.onCompletePingTargets", false),
        PUBLIC_SUBSCRIPTION_FEED("rinfo.main.publicSubscriptionFeed"),
        COLLECTOR_LOG_DATA_DIR("rinfo.main.collector.logDataDir"),
        COMPLETE_FEEDS_ID_INDEX_DIR("rinfo.main.collector.completeFeedsIndexDir"),
        WHITELISTED_FEEDS("rinfo.main.collector.whiteListedFeed");

        String value;
        boolean requiredValue = true;
        private ConfigKey(String value) { this.value = value; }
        private ConfigKey(String value, boolean requiredValue) {
            this.value = value;
            this.requiredValue = requiredValue;
        }
        String toString() { return value; }
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

    Components(Configuration config) {
        this.config = config
        checkConfig()
    }

    Configuration getConfig() { return config; }
    Storage getStorage() { return storage; }
    CollectorLog getCollectorLog() { return collectorLog; }
    FeedCollectScheduler getCollectScheduler() { return collectScheduler; }
    FeedCollector getFeedCollector() { return feedCollector; }

    void bootstrap() {
        setupCollectorLog()
        setupStorage()
        setupFeedCollector()
        setupCollectScheduler()
        setupStorageHandlers()
    }

    void startup() {
        storage.startup()
        collectScheduler.startup()
    }

    void shutdown() {
        try {
            collectScheduler.shutdown()
        } finally {
            storage.shutdown()
        }
    }

    protected void checkConfig() throws ConfigurationException {
        List<String> missingKeys = new ArrayList<String>();
        for (ConfigKey cKey : ConfigKey.values()) {
            if (!config.containsKey(cKey.toString()) ||
                cKey.requiredValue && StringUtils.isEmpty(configString(cKey))) {
                missingKeys.add(cKey.toString());
            }
        }
        if (!missingKeys.isEmpty()) {
            throw new ConfigurationException("Configuration " + config +
                    " is missing the required keys: " + missingKeys.join(", "));
        }
    }

    protected void configure(Object bean, String subsetPrefix) {
        BeanUtils.populate(bean,
                new ConfigurationMap(config.subset(subsetPrefix)))
    }

    String configString(ConfigKey key) {
        return config.getString(key.value);
    }

    private void setupCollectorLog() {
        collectorLog = new CollectorLog(createRegistryRepo(),
                configString(ConfigKey.REPORT_BASE_URI),
                configString(ConfigKey.SYSTEM_DATASET_URI))
    }

    protected void setupStorage() {
        storage = new Storage(createDepot(), collectorLog,
                createFeedEntryDataIndex())
    }

    protected void setupFeedCollector() {
        feedCollector = new FeedCollector(storage,
                config.getInt(ConfigKey.COLLECTOR_HTTP_TIMEOUT_SECONDS.value))
        if (config.getBoolean(ConfigKey.COLLECTOR_HTTP_ALLOW_SELF_SIGNED.value)) {
            feedCollector.allowSelfSigned = true
        }
    }

    protected void setupCollectScheduler() {
        collectScheduler = new FeedCollectScheduler(feedCollector)
        configure(collectScheduler, "rinfo.main.collector")
        //collectScheduler.adminFeedId = new URI(configString(ConfigKey.ADMIN_FEED_ID))
        //collectScheduler.adminFeedUrl = new URL(configString(ConfigKey.ADMIN_FEED_URL))

        collectScheduler.whiteListedFeeds = configString(ConfigKey.WHITELISTED_FEEDS).split(';').inject([:]) { map, token ->
            token.split('=').with { map[new URL(it[0])] = new URI(it[1]) }
            map
        }

        def publicSubscriptionFeed = new URL(
                configString(ConfigKey.PUBLIC_SUBSCRIPTION_FEED))
        def onCompletePingTargets = new ArrayList<URL>()
        for (String s : config.getList(
                    ConfigKey.ON_COMPLETE_PING_TARGETS.value)) {
            if (s == null || s.equals("")) continue
            onCompletePingTargets << new URL(s)
        }

        collectScheduler.setAfterLastJobCallback(
                new FeedUpdatePingNotifyer(publicSubscriptionFeed, onCompletePingTargets)
        )
    }

    protected void setupStorageHandlers() {
        storage.setStorageHandlers(createStorageHandlers())
    }

    Depot createDepot() {
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
            for (def iter = depot.iterateLockedEntries(); iter.hasNext();) {
                def lockedEntry = iter.next()
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

    FeedEntryDataIndex createFeedEntryDataIndex() {
        def completeFeedsIdIndexDir = new File(configString(
                ConfigKey.COMPLETE_FEEDS_ID_INDEX_DIR))
        ensureDir(completeFeedsIdIndexDir)
        return new FeedEntryDataIndexFSImpl(completeFeedsIdIndexDir);
    }

    Repository createRegistryRepo() {
        def dataDirPath = configString(ConfigKey.COLLECTOR_LOG_DATA_DIR)
        def dataDir = new File(dataDirPath)
        ensureDir(dataDir)
        def repo = new SailRepository(new NativeStore(dataDir))
        repo.initialize()
        return repo
    }

    Collection<StorageHandler> createStorageHandlers() {
        def storageHandlers = new ArrayList<StorageHandler>()
        storageHandlers.add(createSourceFeedsConfigHandler())
        storageHandlers.add(createEntryRdfValidatorHandler())
        return storageHandlers
    }

    SourceFeedsConfigHandler createSourceFeedsConfigHandler() {
        return new SourceFeedsConfigHandler(
                collectScheduler,
                new URI(configString(ConfigKey.SOURCE_FEEDS_ENTRY_ID)),
                new URI(configString(ConfigKey.SYSTEM_DATASET_URI)))
    }

    EntryRdfValidatorHandler createEntryRdfValidatorHandler() {
        return new EntryRdfValidatorHandler(
                configString(ConfigKey.CHECKER_CHECKED_BASE_PATH),
                config.getList(ConfigKey.CHECKER_VOCAB_ENTRY_IDS.value),
                configString(ConfigKey.VALIDATION_ENTRY_ID),
                configString(ConfigKey.URIMINTER_ENTRY_ID),
                configString(ConfigKey.URIMINTER_URI_SPACE_URI))
    }

    private void ensureDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Cannot create directory: " + dir);
            }
        }
    }
}
