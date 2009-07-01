package se.lagrummet.rinfo.collector

import org.junit.After
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*

import java.util.concurrent.Semaphore


class AbstractCollectSchedulerTest {

    static SOURCE_FEEDS = [
        [url: new URL("http://source1.example.org/"), items:["1a", "1b"]],
        [url: new URL("http://source2.example.org/"), items:["2a", "2b"]],
    ]

    AbstractCollectScheduler collectScheduler

    static int SAFE_STARTUP_MILLIS = 100

    @Before
    void startUp() {
        collectScheduler = new ManagedDummyScheduler(
            initialDelay: 0,
            scheduleInterval: -1,
            timeUnitName: "MILLISECONDS",
            sourceFeedUrls: SOURCE_FEEDS.collect { it.url }
        )
    }

    @After
    void tearDown() {
        collectScheduler.shutdown()
    }

    @Test
    void shouldTriggerCollect() {
        collectScheduler.startup()
        def fakeSource = SOURCE_FEEDS[0]
        assertTrue collectScheduler.triggerFeedCollect(fakeSource.url)
        collectScheduler.waitForCompletedCollect()
        assertEquals fakeSource.items, collectScheduler.collectedItems
    }

    @Test
    void shouldCollectAllFeeds() {
        collectScheduler.startup()
        def fakeSource = SOURCE_FEEDS[0]
        collectScheduler.collectAllFeeds()
        collectScheduler.waitForCompletedCollect()
        assertEquals SOURCE_FEEDS.collect { it.items }.flatten(),
                collectScheduler.collectedItems
    }

    /*
    @Test
    void shouldNotTriggerWhenRunningScheduled() {
        collectScheduler.scheduleInterval = 20
        collectScheduler.pause()
        collectScheduler.startup()
        Thread.sleep(SAFE_STARTUP_MILLIS)
        assertFalse "Expected stalled collector to block trigger.",
                collectScheduler.triggerFeedCollect(SOURCE_FEEDS[1].url)
        collectScheduler.unpause()
    }
    */

    /*
    @Test
    void shouldNeverCollectConcurrently() {
        collectScheduler.scheduleInterval = 20
        collectScheduler.pause()
        collectScheduler.startup()
        Thread.sleep(SAFE_STARTUP_MILLIS)
        assertFalse "Expected stalled collector to block new collectAll.",
                collectScheduler.collectAllFeeds()
        collectScheduler.unpause()
    }
    */

    @Test(expected=NotAllowedSourceFeedException)
    void shouldFailOnDisallowedSourceUrl() {
        collectScheduler.startup()
        collectScheduler.triggerFeedCollect(new URL("http://bad.example.org/"))
    }

    @Test
    void shouldBeRestartable() {
        collectScheduler.scheduleInterval = 20

        collectScheduler.startup()
        assertTrue collectScheduler.isStarted()

        collectScheduler.shutdown()
        assertTrue collectScheduler.getScheduleService().isShutdown()
        assertTrue collectScheduler.getExecutorService().isShutdown()
        assertFalse collectScheduler.isStarted()

        collectScheduler.startup()
        assertTrue collectScheduler.isStarted()
        assertFalse collectScheduler.getScheduleService().isShutdown()
        assertFalse collectScheduler.getExecutorService().isShutdown()
    }
}

class ManagedDummyScheduler extends AbstractCollectScheduler {

    Collection sourceFeedUrls

    def collectedItems = []

    private reachedLastSemaphore = new Semaphore(1)

    protected waitForCompletedCollect() {
        reachedLastSemaphore.acquire()
        reachedLastSemaphore.release()
    }

    private blockCollectSemaphore

    protected pause() {
        blockCollectSemaphore = new Semaphore(1)
        blockCollectSemaphore.acquire()
    }

    protected unpause() {
        blockCollectSemaphore.release()
        blockCollectSemaphore = null
    }

    void collectFeed(URL feedUrl, boolean lastInBatch) {
        if (blockCollectSemaphore) {
            blockCollectSemaphore.acquire()
            blockCollectSemaphore.release()
        }
        AbstractCollectSchedulerTest.SOURCE_FEEDS.find {
                it.url == feedUrl
        }.items.each {
            collectedItems << it
        }
        if (lastInBatch) {
            reachedLastSemaphore.release()
        }
    }

    void collectAllFeeds() {
        reachedLastSemaphore.tryAcquire()
        super.collectAllFeeds()
    }

    public boolean triggerFeedCollect(final URL feedUrl)
            throws NotAllowedSourceFeedException {
        reachedLastSemaphore.tryAcquire()
        return super.triggerFeedCollect(feedUrl)
    }

}
