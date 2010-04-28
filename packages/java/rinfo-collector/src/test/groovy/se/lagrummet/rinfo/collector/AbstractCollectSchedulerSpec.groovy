package se.lagrummet.rinfo.collector

import spock.lang.*

import java.util.concurrent.Semaphore


class AbstractCollectSchedulerSpec extends Specification {

    static SOURCE_FEEDS = [
        [url: new URL("http://source1.example.org/"), items:['1a', '1b']],
        [url: new URL("http://source2.example.org/"), items:['2a', '2b']],
    ]

    AbstractCollectScheduler collectScheduler

    void setup() {
        collectScheduler = new ManagedDummyScheduler(
            initialDelay: 0,
            scheduleInterval: -1,
            timeUnitName: "MILLISECONDS",
            sourceFeedUrls: SOURCE_FEEDS.collect { it.url }
        )
    }

    void cleanup() {
        collectScheduler.shutdown()
    }

    def "should trigger collect"() {
        when:
        collectScheduler.startup()
        def fakeSource = SOURCE_FEEDS[0]
        then:
        assert collectScheduler.triggerFeedCollect(fakeSource.url)

        when:
        collectScheduler.waitForCompletedCollect()
        then:
        Thread.sleep 1
        fakeSource.items == collectScheduler.collectedItems
    }

    def "should collect all feeds"() {
        setup:
        collectScheduler.feedsToWaitFor = SOURCE_FEEDS.size()
        when:
        collectScheduler.startup()
        collectScheduler.collectAllFeeds()
        collectScheduler.waitForCompletedCollect()
        then:
        SOURCE_FEEDS.collect { it.items }.flatten() == collectScheduler.collectedItems
    }

    def "should not trigger when running scheduled"() {
        setup:
        collectScheduler.scheduleInterval = 20
        when:
        collectScheduler.pause()
        collectScheduler.startup()
        Thread.sleep(collectScheduler.initialDelay + 100)
        then: "Expected stalled collector to block trigger."
        ! collectScheduler.triggerFeedCollect(SOURCE_FEEDS[1].url)
        cleanup:
        collectScheduler.unpause()
    }

    /*
    def "should never collect concurrently"() {
        ...
        ! "Expected stalled collector to block new collectAll.", ...
    }
    */

    def "should fail on disallowed source url"() {
        when:
        collectScheduler.startup()
        collectScheduler.triggerFeedCollect(new URL("http://bad.example.org/"))
        then:
        thrown(NotAllowedSourceFeedException)
    }

    def "should be restartable"() {
        setup:
        collectScheduler.scheduleInterval = 20
        when:
        collectScheduler.startup()
        then:
        assert collectScheduler.isStarted()

        when:
        collectScheduler.shutdown()
        then:
        assert collectScheduler.getScheduleService().isShutdown()
        assert collectScheduler.getExecutorService().isShutdown()
        ! collectScheduler.isStarted()

        when:
        collectScheduler.startup()
        then:
        assert collectScheduler.isStarted()
        ! collectScheduler.getScheduleService().isShutdown()
        ! collectScheduler.getExecutorService().isShutdown()
    }
}


class ManagedDummyScheduler extends AbstractCollectScheduler {

    Collection sourceFeedUrls

    def collectedItems = []

    int feedVisitCount
    int feedsToWaitFor
    private reachedLastSemaphore = new Semaphore(1)
    private blockCollectSemaphore

    void collectFeed(URL feedUrl, boolean lastInBatch) {
        if (blockCollectSemaphore) {
            blockCollectSemaphore.acquire()
            blockCollectSemaphore.release()
        }
        AbstractCollectSchedulerSpec.SOURCE_FEEDS.find {
                it.url == feedUrl
        }.items.each {
            collectedItems << it
        }
        feedVisitCount += 1
        if (!feedsToWaitFor && lastInBatch
            || feedVisitCount == feedsToWaitFor) {
            reachedLastSemaphore.release()
        }
    }

    void collectAllFeeds() {
        reachedLastSemaphore.tryAcquire()
        super.collectAllFeeds()
    }

    public boolean triggerFeedCollect(final URL feedUrl)
            throws NotAllowedSourceFeedException {
        return super.triggerFeedCollect(feedUrl)
    }

    protected waitForCompletedCollect() {
        reachedLastSemaphore.acquire()
        reachedLastSemaphore.release()
    }

    protected pause() {
        blockCollectSemaphore = new Semaphore(1)
        blockCollectSemaphore.acquire()
    }

    protected unpause() {
        blockCollectSemaphore.release()
        blockCollectSemaphore = null
    }

}
