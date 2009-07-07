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

import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler
import se.lagrummet.rinfo.main.storage.FeedCollectScheduler
import se.lagrummet.rinfo.main.storage.FeedCollectorRegistry
import se.lagrummet.rinfo.main.storage.Storage
import se.lagrummet.rinfo.main.storage.StorageHandler
import se.lagrummet.rinfo.main.storage.StorageSession
import se.lagrummet.rinfo.main.storage.SourceFeedsConfigHandler


public static enum ConfigKey {

    CONTAINER_DESCRIPTION_ENTRY_ID("rinfo.main.uriMinter.containerDescriptionEntryId"),
    SOURCE_FEEDS_ENTRY_ID("rinfo.main.storage.sourceFeedsEntryId"),
    ON_COMPLETE_PING_TARGETS("rinfo.main.collector.onCompletePingTargets"),
    PUBLIC_SUBSCRIPTION_FEED("rinfo.main.publicSubscriptionFeed"),
    REGISTRY_DATA_DIR("rinfo.main.collector.registryDataDir");

    private final String value;
    private ConfigKey(String value) { this.value = value; }
    public String toString() { return value; }
}

public class Components {

    Storage storage
    FeedCollectScheduler collectScheduler

    static {
        BeanUtilsURIConverter.registerIfNoURIConverterIsRegistered()
    }

    private Configuration config

    public Components(Configuration config) {
        this.config = config
        checkConfig()
        setupStorage()
        setupCollectScheduler()
        setupStorageHandlers()
    }

    public Configuration getConfig() { return config }

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
        storage = new Storage(createDepot(), createRegistryRepo())
    }

    protected void setupCollectScheduler() {
        collectScheduler = new FeedCollectScheduler(storage)
        configure(collectScheduler, "rinfo.main.collector")
        def publicSubscriptionFeed = new URL(
                config.getString(ConfigKey.PUBLIC_SUBSCRIPTION_FEED.toString()))
        def onCompletePingTargets = config.getList(
                ConfigKey.ON_COMPLETE_PING_TARGETS.toString()).collect { new URL(it) }
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
        /* TODO: rework - undepend pathHandler and atomizer, then use:
        depot = new FileDepot()
        depot.setPathHandler(new MainPathHandler())
        depot.setIndexer(new AtomDepotIndexer())
        configure(depot, "rinfo.main.depot")
        depot.initialize()
        */
        return depot
    }

    private Repository createRegistryRepo() {
        def dataDirPath = config.getString(ConfigKey.REGISTRY_DATA_DIR.toString())
        def dataDir = new File(dataDirPath)
        if (!dataDir.exists()) {
            dataDir.mkdir()
        }
        return new SailRepository(new NativeStore(dataDir))
    }

    private Collection<StorageHandler> createStorageHandlers() {
        def storageHandlers = new ArrayList<StorageHandler>()
        storageHandlers.add(createSourceFeedsConfigHandler())
        storageHandlers.add(createEntryRdfValidatorHandler())
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
