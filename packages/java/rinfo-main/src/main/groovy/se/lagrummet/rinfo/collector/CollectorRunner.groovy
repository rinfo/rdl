package se.lagrummet.rinfo.collector

import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.collector.FeedCollector


class CollectorRunner extends CollectorRunnerBase {

    private final Logger logger = LoggerFactory.getLogger(CollectorRunner.class);

    private Collection sourceFeedUrls

    private FileDepot depot
    private Repository registryRepo
    private URIMinter uriMinter

    CollectorRunner(FileDepot depot, URIMinter uriMinter,
            AbstractConfiguration config) {
        this(depot, uriMinter)
        this.configure(config)
    }

    CollectorRunner(FileDepot depot, URIMinter uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
    }

    void configure(AbstractConfiguration config) {

        setInitialDelay(config.getInt(
                "rinfo.collector.initialDelay", DEFAULT_INITIAL_DELAY))
        setScheduleInterval(config.getInt(
                "rinfo.collector.scheduleInterval", DEFAULT_SCHEDULE_INTERVAL))
        setTimeUnitName(config.getString(
                "rinfo.collector.timeUnit", DEFAULT_TIME_UNIT_NAME))

        if (depot == null) {
            depot = FileDepot.newConfigured(config)
        }
        if (uriMinter == null) {
            uriMinter = new URIMinter(config.getString("rinfo.main.baseDir"))
        }
        if (registryRepo == null) {
            def dataDirPath = config.getString("rinfo.collector.registryDataDir")
            def dataDir = new File(dataDirPath)
            if (!dataDir.exists()) {
                dataDir.mkdir()
            }
            registryRepo = new SailRepository(new NativeStore(dataDir))
            registryRepo.initialize()
        }
        sourceFeedUrls = config.getList("rinfo.collector.sourceFeedUrls")
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

    protected void collectFeed(URL feedUrl) {
        //  .. and (in webapp) that request comes from allowed domain..
        FeedCollector.readFeed(depot, registryRepo, uriMinter, feedUrl)
    }

}
