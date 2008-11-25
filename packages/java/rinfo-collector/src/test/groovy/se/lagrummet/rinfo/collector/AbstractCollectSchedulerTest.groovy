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

    AbstractCollectScheduler runner

    static int SAFE_STARTUP_MILLIS = 100

    @Before
    void startUp() {
        runner = new DummyRunner(
            initialDelay: 0,
            scheduleInterval: -1,
            timeUnitName: "MILLISECONDS",
            sourceFeedUrls: SOURCE_FEEDS.collect { it.url }
        )
    }

    @After
    void tearDown() {
        runner.shutdown()
    }

    @Test
    void shouldTriggerCollect() {
        runner.startup()
        def fakeSource = SOURCE_FEEDS[0]
        assertTrue runner.triggerFeedCollect(fakeSource.url)
        Thread.sleep(50)
        assertEquals fakeSource.items, runner.collectedItems
    }

    @Test
    void shouldNotTriggerWhenRunningScheduled() {
        runner.scheduleInterval = 200
        runner.sleepBefore = SAFE_STARTUP_MILLIS*2
        runner.startup()
        try {
            Thread.sleep(SAFE_STARTUP_MILLIS)
            assertTrue "Expected stall to have ocurred at least once.",
                    runner.triggerFeedCollect(SOURCE_FEEDS[1].url)
        } finally {
            runner.shutdown()
        }
    }

    @Test
    void shouldNeverCollectConcurrently() {
        runner.scheduleInterval = 200
        runner.sleepOnce = SAFE_STARTUP_MILLIS*2
        runner.startup()
        Thread.sleep(SAFE_STARTUP_MILLIS)
        assertFalse runner.collectAllFeeds()
        Thread.sleep(SAFE_STARTUP_MILLIS)
    }

}

class DummyRunner extends AbstractCollectScheduler {

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
