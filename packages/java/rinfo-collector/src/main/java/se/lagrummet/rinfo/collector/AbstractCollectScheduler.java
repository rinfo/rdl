package se.lagrummet.rinfo.collector;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
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
    private TimeUnit timeUnit = TimeUnit.valueOf(DEFAULT_TIME_UNIT_NAME);

    private boolean started = false;

    private ScheduledExecutorService scheduleService;

    private ExecutorService executorService;

    // NOTE: Why String not URL in these? Take a long, hard look at the javadoc
    // for java.net.URL#equals!

    private ConcurrentLinkedQueue<String> feedQueue =
        new ConcurrentLinkedQueue<String>();

    private List<String> feedInProcess =
        Collections.synchronizedList(new ArrayList<String>());

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
        this.timeUnit = TimeUnit.valueOf(timeUnitName.toUpperCase());
    }

    public boolean isStarted() {
        return started;
    }

    public ScheduledExecutorService getScheduleService() {
        return scheduleService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     *  Creates a single threaded executor by default. Override to implement
     *  other thread executor strategy.
     */
    public ExecutorService newExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * @return Collection of approved source feed URLs.
     */
    public abstract Collection<URI> getSourceFeedUrls();

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
        feedInProcess.clear();
        feedQueue.clear();
        executorService = newExecutorService();
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
        started = true;
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
        executorService.shutdown();

        if (feedQueue != null) {
            if (feedQueue.size() > 0) {
                StringBuffer buf = new StringBuffer();
                for (String u : feedQueue) {
                    buf.append("<"+u+">, ");
                }
                String feeds = buf.toString();
                feedQueue.clear();
                StringUtils.removeEnd(feeds, ", ");
                logger.info("Shutdown prevented the following scheduled feeds to "
                        + "be collected: " + feeds);
            }
        }
        started = false;
    }

    /**
     * Schedule all source feeds for collect.
     */
    public void collectAllFeeds() {
        Collection<URI> sourceFeedUrls = getSourceFeedUrls();
        if (sourceFeedUrls != null) {
            logger.info("Starting scheduling for collect of " +
                    sourceFeedUrls.size() + " source feeds.");
            for (URI feedUrl : sourceFeedUrls) {
                try {
                    enqueueCollect(feedUrl.toURL());
                } catch (MalformedURLException ignore) {} // Ignored because will be checked before this serveral times
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
        try {
            if (!getSourceFeedUrls().contains(feedUrl.toURI())) {
                throw new NotAllowedSourceFeedException(
                        "Called triggerFeedCollect with disallowed " +
                        "feed url: <"+ feedUrl +">");
            }
        } catch (URISyntaxException ignore) {}
        return enqueueCollect(feedUrl);
    }

    private synchronized boolean enqueueCollect(final URL feedUrl) {
        String feedUrlStr = feedUrl.toString();
        if (feedQueue.contains(feedUrlStr)) {
            logger.info("Feed <"+ feedUrlStr +"> is already scheduled for collect.");
            return false;
        } else if (feedInProcess.contains(feedUrlStr)) {
            logger.info("Feed <"+ feedUrlStr +"> is already being collected.");
            return false;
        } else {
            feedQueue.add(feedUrlStr);
            logger.info("Scheduling collect of <"+ feedUrlStr +">.");
            executorService.execute(
                    new Runnable() {
                        public void run() { executeCollect(); }
                    }
            );
            return true;
        }
    }

    private void executeCollect() {
        String feedUrlStr = getNextFeed();
        if (feedUrlStr != null) {
            boolean initialAdminReadOnEmptySystem = (getSourceFeedUrls().size() == 2);
            try {
                collectFeed(new URL(feedUrlStr), true);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } finally {
                feedInProcess.remove(feedUrlStr);
            }
            ifInitialAdminReadThenEnqueueAllNewSources(feedUrlStr, initialAdminReadOnEmptySystem);

            afterCompletedCollect(feedUrlStr);
        }
    }

    private void ifInitialAdminReadThenEnqueueAllNewSources(String feedUrlStr, boolean initialAdminReadOnEmptySystem) {
        if (initialAdminReadOnEmptySystem && getSourceFeedUrls().size() > 2) try {
            for (URI sourceFeedURI : getSourceFeedUrls()) {
                if (!sourceFeedURI.equals(new URL(feedUrlStr).toURI()))
                    enqueueCollect(sourceFeedURI.toURL());
            }
        } catch (Exception ignore) {ignore.printStackTrace();}
    }

    protected abstract void afterCompletedCollect(String feedUrlStr);

    private synchronized String getNextFeed() {
        String feedUrlStr = feedQueue.peek();
        if (feedUrlStr != null) {
            feedInProcess.add(feedUrlStr);
            feedQueue.remove(feedUrlStr);
        }
        return feedUrlStr;
    }

    public synchronized boolean areJobQueuesEmpty() {
        logger.debug("Checking if job queues are empty");
        return feedInProcess.isEmpty() && feedQueue.isEmpty();
    }

    public Map status(Map report) {
        report.put("started", Boolean.toString(started));
        report.put("feedQueue.size", feedQueue.size());
        report.put("feedQueue", feedQueue);
        report.put("feedInProcess.size", feedInProcess.size());
        report.put("feedInProcess", feedInProcess);
        return report;
    }
}
