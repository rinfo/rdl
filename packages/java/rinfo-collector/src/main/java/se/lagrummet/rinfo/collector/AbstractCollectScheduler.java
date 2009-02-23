package se.lagrummet.rinfo.collector;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(AbstractCollectScheduler.class);

    public static final int DEFAULT_INITIAL_DELAY = 0;
    public static final int DEFAULT_SCHEDULE_INTERVAL = 600;
    public static final String DEFAULT_TIME_UNIT_NAME = "SECONDS";

    private int initialDelay = DEFAULT_INITIAL_DELAY;
    private int scheduleInterval = DEFAULT_SCHEDULE_INTERVAL;
    private TimeUnit timeUnit;

    private ScheduledExecutorService scheduleService;

    private ExecutorService defaultExecutorService =
        Executors.newSingleThreadExecutor();

    private ConcurrentLinkedQueue<URL> feedQueue =
        new ConcurrentLinkedQueue<URL>();

    private List<URL> feedInProcess =
        Collections.synchronizedList(new ArrayList<URL>());

    public int getInitialDelay() {
        return initialDelay;
    }
    public void setInitialDelay(int initialDelay) {
        this.initialDelay = initialDelay;
    }

    public int getScheduleInterval() {
        return scheduleInterval;
    }
    public void setScheduleInterval(int scheduleInterval) {
        this.scheduleInterval = scheduleInterval;
    }

    public String getTimeUnitName() {
        return timeUnit.toString();
    }
    public void setTimeUnitName(String timeUnitName) {
        this.timeUnit = TimeUnit.valueOf(timeUnitName);
    }

    /**
     * Get default single threaded executor. Override to implement other thread
     * executor strategy.
     * @return
     */
    public ExecutorService getExecutorService() {
        return defaultExecutorService;
    }

    /**
     * @return Collection of approved source feed URLs.
     */
    public abstract Collection<URL> getSourceFeedUrls();

    /**
     * Perform collect of feed from the given URL.
     * @param feedUrl
     * @param lastInBatch
     */
    protected abstract void collectFeed(URL feedUrl, boolean lastInBatch);

    /**
     * Initiate a fixed rate schedule for collecting all feeds, with an interval
     * of <code>scheduleInterval</code>, unless it is set to -1.
     */
    public void startup() {
        if (scheduleInterval == -1) {
            logger.info("Disabled scheduled collects.");
        } else {
            scheduleService = Executors.newSingleThreadScheduledExecutor();
            scheduleService.scheduleAtFixedRate(
                new Runnable() {
                  public void run() { collectAllFeeds(); }
                } ,
                initialDelay, scheduleInterval, timeUnit);
            String unitName = timeUnit.toString().toLowerCase();
            logger.info("Scheduled collect every "+scheduleInterval +
                    " "+unitName+" (starting in "+initialDelay+" "+unitName+").");
        }
    }

    /**
     * Shuts down the fixed rate collect schedule and aborts all queued up
     * collects that have not yet started. All currently running collects are
     * allowed to finish.
     */
    public void shutdown() {
        if (scheduleService != null) {
            scheduleService.shutdown();
        }

        getExecutorService().shutdown();

        if (feedQueue != null) {
            if (feedQueue.size() > 0) {
                String feeds = "";
                for (URL u : feedQueue) {
                    feeds += "<"+u+">, ";
                }
                feedQueue.clear();
                StringUtils.removeEnd(feeds, ", ");
                logger.info("Shutdown prevented the following scheduled feeds to "
                        + "be collected: " + feeds);
            }
        }
    }

    /**
     * Schedule all source feeds for collect.
     */
    public void collectAllFeeds() {
        Collection<URL> sourceFeedUrls = getSourceFeedUrls();
        if (sourceFeedUrls != null) {
            logger.info("Starting scheduling for collect of " +
                    sourceFeedUrls.size() + " source feeds.");
            for (URL feedUrl : sourceFeedUrls) {
                enqueueCollect(feedUrl);
            }
            logger.info("Done scheduling source feeds.");
        } else {
            logger.info("No source feeds to schedule.");
        }
    }

    /**
     * Schedule feed to be collected.
     *
     * @param feedUrl The URL of the feed to collect.
     * @return true if the URL was added to the queue, false if it already was
     * enqueued.
     * @throws NotAllowedSourceFeedException if feed URL is not on the list of
     * approved sources.
     */
    public boolean triggerFeedCollect(final URL feedUrl)
            throws NotAllowedSourceFeedException {
        if (!getSourceFeedUrls().contains(feedUrl)) {
            throw new NotAllowedSourceFeedException(
                    "Called triggerFeedCollect with disallowed " +
                    "feed url: <"+feedUrl+">");
        }
        return enqueueCollect(feedUrl);
    }

    private synchronized boolean enqueueCollect(final URL feedUrl) {
        if (feedQueue.contains(feedUrl) || feedInProcess.contains(feedUrl)) {
            logger.info("Feed <"+feedUrl+"> is already scheduled for collect.");
            return false;
        } else {
            feedQueue.add(feedUrl);
            logger.info("Scheduling collect of <"+feedUrl+">.");
            getExecutorService().execute(
                    new Runnable() {
                        public void run() { executeCollect(); }
                    }
            );
            return true;
        }
    }

    private synchronized URL getNextFeed() {
        URL feedUrl = feedQueue.peek();
        if (feedUrl != null) {
            feedInProcess.add(feedUrl);
            feedQueue.remove(feedUrl);
        }
        return feedUrl;
    }

    private void executeCollect() {
        URL feedUrl = getNextFeed();
        if (feedUrl != null) {
            try {
                collectFeed(feedUrl, true);
            } finally {
                feedInProcess.remove(feedUrl);
            }
            logger.info("Completed collect of <"+feedUrl+">.");
        }
    }
}
