package se.lagrummet.rinfo.main

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class FeedCollectScheduler extends AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(FeedCollectScheduler.class);

    private Collection<URL> sourceFeedUrls

    private DataHub dataHub

    Runnable batchCompletedCallback

    FeedCollectScheduler(DataHub dataHub, Configuration config) {
        this(dataHub)
        this.configure(config)
    }

    FeedCollectScheduler(DataHub dataHub) {
        this.dataHub = dataHub
    }

    void configure(Configuration config) {
        setInitialDelay(config.getInt(
                "rinfo.main.collector.initialDelay", DEFAULT_INITIAL_DELAY))
        setScheduleInterval(config.getInt(
                "rinfo.main.collector.scheduleInterval", DEFAULT_SCHEDULE_INTERVAL))
        setTimeUnitName(config.getString(
                "rinfo.main.collector.timeUnit", DEFAULT_TIME_UNIT_NAME))
        sourceFeedUrls = new ArrayList<URL>()
        for (String url : config.getList("rinfo.main.collector.sourceFeedUrls")) {
            sourceFeedUrls.add(new URL(url))
        }
    }

    public Collection getSourceFeedUrls() {
        return sourceFeedUrls
    }

    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        FeedCollector.readFeed(depot, registryRepo, uriMinter, feedUrl)
        if (batchCompletedCallback != null) {
            batchCompletedCallback.run()
        }
    }

}
