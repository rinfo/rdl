package se.lagrummet.rinfo.collector

import org.junit.After
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*


class AbstractCollectSchedulerTest {

    static SOURCE_FEEDS = [
        [url: new URL("http://source1.example.org/"), items:["1a", "1b"]],
        [url: new URL("http://source2.example.org/"), items:["2a", "2b"]],
    ]

    AbstractCollectScheduler collectScheduler

    static int SAFE_STARTUP_MILLIS = 100

    @Before
    void startUp() {
        collectScheduler = new DummyScheduler(
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
        Thread.sleep(50)
        assertEquals fakeSource.items, collectScheduler.collectedItems
    }

    @Test
    void shouldNotTriggerWhenRunningScheduled() {
        collectScheduler.scheduleInterval = 200
        collectScheduler.sleepBefore = SAFE_STARTUP_MILLIS*2
        collectScheduler.startup()
        try {
            Thread.sleep(SAFE_STARTUP_MILLIS)
            assertTrue "Expected stall to have ocurred at least once.",
                    collectScheduler.triggerFeedCollect(SOURCE_FEEDS[1].url)
        } finally {
            collectScheduler.shutdown()
        }
    }

    @Test
    void shouldNeverCollectConcurrently() {
        collectScheduler.scheduleInterval = 200
        collectScheduler.sleepOnce = SAFE_STARTUP_MILLIS*2
        collectScheduler.startup()
        Thread.sleep(SAFE_STARTUP_MILLIS)
        assertFalse collectScheduler.collectAllFeeds()
        Thread.sleep(SAFE_STARTUP_MILLIS)
    }

}

class DummyScheduler extends AbstractCollectScheduler {

    Collection sourceFeedUrls

    def collectedItems
    def sleepBefore
    def sleepOnce

    void collectFeed(URL feedUrl) {
        if (sleepOnce > 0) {
            Thread.sleep(sleepOnce)
            sleepOnce = 0
        }
        collectedItems = AbstractCollectSchedulerTest.SOURCE_FEEDS.find {
                it.url == feedUrl
            }.items
    }

    boolean collectAllFeeds() {
        if (sleepBefore > 0) Thread.sleep(sleepBefore)
        super.collectAllFeeds()
    }

}
