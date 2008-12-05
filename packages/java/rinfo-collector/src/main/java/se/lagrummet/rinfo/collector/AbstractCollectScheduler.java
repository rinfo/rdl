package se.lagrummet.rinfo.collector;

import java.util.*;
import java.net.URL;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(AbstractCollectScheduler.class);

    public static final int DEFAULT_INITIAL_DELAY = 0;
    public static final int DEFAULT_SCHEDULE_INTERVAL = 600;
    public static final String DEFAULT_TIME_UNIT_NAME = "SECONDS";

    private int initialDelay = DEFAULT_INITIAL_DELAY;
    private int scheduleInterval = DEFAULT_SCHEDULE_INTERVAL;
    private String timeUnitName;

    private TimeUnit timeUnit;
    private Semaphore semaphore = new Semaphore(1);

    private ScheduledExecutorService scheduleService;

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

    public abstract Collection<URL> getSourceFeedUrls();

    // TODO:IMPROVE: does the current semaphore work? Consider:
    //  - demand collect/write concurrenct safety in user instead?
    //  - pop from synchronized queue? (To e.g. inform if triggerFeedCollect
    //    will "soon" tart collecting?)
    protected abstract void collectFeed(URL feedUrl, boolean lastInBatch);

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

    public void shutdown() {
        if (scheduleService != null) {
            scheduleService.shutdown();
        }
    }

    public boolean triggerFeedCollect(final URL feedUrl)
            throws NotAllowedSourceFeedException {
        if (!getSourceFeedUrls().contains(feedUrl)) {
            throw new NotAllowedSourceFeedException(
                    "Called triggerFeedCollect with disallowed " +
                    "feed url: <"+feedUrl+">");
        }
        if (!semaphore.tryAcquire()) {
            logger.info("Busy - skipping collect of <"+feedUrl+">");
            return false;
        }
        try {
            logger.info("Scheduling collect of <${feedUrl}>.");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(
                  new Runnable() {
                    public void run() { collectFeed(feedUrl, true); }
                  }
                );
            executor.shutdown();
            return true;
        } finally {
            semaphore.release();
        }
    }

    public boolean collectAllFeeds() {
        Collection<URL> sourceFeedUrls = getSourceFeedUrls();
        if (sourceFeedUrls == null) {
            return true;
        }
        if (!semaphore.tryAcquire()) {
            logger.info("Busy - skipping collect of all source feeds.");
            return false;
        }
        try {
            logger.info("Starting to collect " + sourceFeedUrls.size() +
                    " source feeds.");
            int count = 0;
            for (URL feedUrl : sourceFeedUrls) {
                count++;
                logger.info("Beginning collect of <"+feedUrl+">.");
                collectFeed(feedUrl, count == sourceFeedUrls.size());
                logger.info("Completed collect of <"+feedUrl+">.");
            }
            logger.info("Done collecting source feeds.");
            return true;
        } finally {
            semaphore.release();
        }
    }

}
