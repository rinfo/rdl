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

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.collector.FeedCollector


class CollectorRunner {

    private final Logger logger = LoggerFactory.getLogger(CollectorRunner.class);

    FileDepot depot
    Collection sourceFeedUrls

    // TODO: configurable
    static final int DEFAULT_POOL_SIZE = 100
    static final int DEFAULT_INITIAL_DELAY = 0
    static final int DEFAULT_SCHEDULE_SECONDS = 4

    private URIMinter uriMinter
    private ScheduledExecutorService execPool

    CollectorRunner(FileDepot depot, URIMinter uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
    }

    void configure(AbstractConfiguration config) {
        if (depot == null) {
            depot = FileDepot.newConfigured(config)
        }
        if (uriMinter == null) {
            uriMinter = new URIMinter(config.getString("rinfo.main.baseDir"))
        }
        //TODO:sourceFeedUrls = config.getList("rinfo.collector.sourceFeedUrls")
    }

    // FIXME: make sure collects are *never* running simultaneously!

    void startup() {
        execPool = Executors.newScheduledThreadPool(DEFAULT_POOL_SIZE)
        execPool.scheduleAtFixedRate(
            { collectFeeds() }, DEFAULT_INITIAL_DELAY,
                    DEFAULT_SCHEDULE_SECONDS, TimeUnit.SECONDS)
    }

    void shutdown() {
        execPool.shutdown()
    }

    void spawnOneFeedCollect(URL feedUrl) {
        def executor = Executors.newSingleThreadExecutor()
        executor.execute({ collectFeed(feedUrl) })
        executor.shutdown()
    }

    void collectFeed(URL feedUrl) {
        FeedCollector.readFeed(depot, uriMinter, feedUrl)
    }

    private void collectFeeds() {
        if (sourceFeedUrls == null) {
            return
        }
        logger.info("Starting to collect ${sourceFeedUrls.size()} source feeds.")
        for (URL feedUrl : sourceFeedUrls) {
            collectFeed(feedUrl)
        }
        logger.info("Done collecting source feeds.")
    }

}
