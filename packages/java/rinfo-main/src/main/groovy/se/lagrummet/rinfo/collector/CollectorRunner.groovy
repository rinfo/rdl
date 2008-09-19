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

    static final int DEFAULT_INITIAL_DELAY = 0
    static final int DEFAULT_SCHEDULE_INTERVAL = 600
    static final String DEFAULT_TIME_UNIT_NAME = "SECONDS"

    private int initialDelay
    private int scheduleInterval
    private TimeUnit timeUnit

    private URIMinter uriMinter
    private ScheduledExecutorService scheduleService

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
        if (depot == null) {
            depot = FileDepot.newConfigured(config)
        }
        if (uriMinter == null) {
            uriMinter = new URIMinter(config.getString("rinfo.main.baseDir"))
        }
        sourceFeedUrls = config.getList("rinfo.collector.sourceFeedUrls")
        initialDelay = config.getInt(
                "rinfo.collector.initialDelay", DEFAULT_INITIAL_DELAY)
        scheduleInterval = config.getInt(
                "rinfo.collector.scheduleInterval", DEFAULT_SCHEDULE_INTERVAL)
        timeUnit = TimeUnit.valueOf(config.getString(
                "rinfo.collector.timeUnit", DEFAULT_TIME_UNIT_NAME))
    }

    void startup() {
        if (scheduleInterval == -1) {
            logger.info("Disabled scheduled collects.")
        } else {
            // FIXME: error handling (currently silently dies on bad feeds)
            scheduleService = Executors.newSingleThreadScheduledExecutor()
            scheduleService.scheduleAtFixedRate(
                { collectFeeds() }, initialDelay, scheduleInterval, timeUnit)
            String unitName = timeUnit.toString().toLowerCase()
            logger.info("Scheduled collect every "+scheduleInterval +
                    " "+unitName+" (starting in "+initialDelay+" "+unitName+").")
        }
    }

    void shutdown() {
        if (scheduleService != null) {
            scheduleService.shutdown()
        }
    }

    boolean triggerFeedCollect(URL feedUrl) {
        if (!sourceFeedUrls.contains(feedUrl.toString())) {
            // TODO: or throw an exception?
            logger.warn("Warning - triggerFeedCollect called with disallowed " +
                    "feed url: <"+feedUrl+">")
            return false
        }
        def executor = Executors.newSingleThreadExecutor()
        executor.execute({ collectFeed(feedUrl) })
        executor.shutdown()
        return true
    }

    // FIXME: make sure collects are *never* running simultaneously!
    //  .. i.e. sync triggerFeedCollect and scheduled collectFeeds somehow..
    //  .. pop from synchronized queue?
    private void collectFeed(URL feedUrl) {
        //  .. and (in webapp) that request comes from allowed domain..
        FeedCollector.readFeed(depot, uriMinter, feedUrl)
    }

    private void collectFeeds() {
        if (sourceFeedUrls == null) {
            return
        }
        logger.info("Starting to collect ${sourceFeedUrls.size()} source feeds.")
        for (String feedUrl : sourceFeedUrls) {
            collectFeed(new URL(feedUrl))
        }
        logger.info("Done collecting source feeds.")
    }

}
