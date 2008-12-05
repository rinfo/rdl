package se.lagrummet.rinfo.main

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.store.depot.FileDepot

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class FeedCollectScheduler extends AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(FeedCollectScheduler.class);

    private Collection<URL> sourceFeedUrls

    private FileDepot depot
    private Repository registryRepo
    private URIMinter uriMinter

    FeedCollectScheduler(FileDepot depot, URIMinter uriMinter,
            AbstractConfiguration config) {
        this(depot, uriMinter)
        this.configure(config)
    }

    FeedCollectScheduler(FileDepot depot, URIMinter uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
    }

    void configure(AbstractConfiguration config) {

        setInitialDelay(config.getInt(
                "rinfo.main.collector.initialDelay", DEFAULT_INITIAL_DELAY))
        setScheduleInterval(config.getInt(
                "rinfo.main.collector.scheduleInterval", DEFAULT_SCHEDULE_INTERVAL))
        setTimeUnitName(config.getString(
                "rinfo.main.collector.timeUnit", DEFAULT_TIME_UNIT_NAME))

        if (depot == null) {
            depot = FileDepot.newConfigured(config)
        }
        if (uriMinter == null) {
            uriMinter = new URIMinter(config.getString("rinfo.main.baseDir"))
        }
        if (registryRepo == null) {
            def dataDirPath = config.getString("rinfo.main.collector.registryDataDir")
            def dataDir = new File(dataDirPath)
            if (!dataDir.exists()) {
                dataDir.mkdir()
            }
            registryRepo = new SailRepository(new NativeStore(dataDir))
            registryRepo.initialize()
        }
        sourceFeedUrls = new ArrayList<URL>()
        for (String url : config.getList("rinfo.main.collector.sourceFeedUrls")) {
            sourceFeedUrls.add(new URL(url))
        }
    }

    void shutdown() {
        super.shutdown()
        if (registryRepo != null) {
            registryRepo.shutDown()
        }
    }

    public Collection getSourceFeedUrls() {
        return sourceFeedUrls
    }

    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        //  .. and (in webapp) that request comes from allowed domain..
        FeedCollector.readFeed(depot, registryRepo, uriMinter, feedUrl)
    }

}
