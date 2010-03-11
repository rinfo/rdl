package se.lagrummet.rinfo.main.storage

import spock.lang.*


class FeedCollectSchedulerSpeck extends Specification {

    def "collect scheduler should add adminFeedUrl to sourceFeedUrls"() {
        setup:
        def collectScheduler = new FeedCollectScheduler(null)
        when:
        collectScheduler.adminFeedUrl = adminUrl
        collectScheduler.publicSourceFeedUrls = publicUrls
        then:
        def adminUrls = adminUrl? [adminUrl] : []
        collectScheduler.sourceFeedUrls == adminUrls + publicUrls
        where:
        adminUrl << [
            null,
            new URL("http://localhost/admin"),
            new URL("http://localhost/admin"),
            null,
        ]
        publicUrls << [
            [new URL("http://localhost/pub/1"), new URL("http://localhost/pub/2")],
            [new URL("http://localhost/pub/1"), new URL("http://localhost/pub/2")],
            [],
            [],
        ]
    }

    def "collect scheduler should authen adminFeedUrl in credentials"() {
        setup:
        def collectScheduler = new FeedCollectScheduler(null)
        def adminUrl = new URL("http://localhost/admin")
        collectScheduler.adminFeedUrl = adminUrl
        expect:
        collectScheduler.newStorageCredentials(adminUrl).isAdmin() == true
        collectScheduler.newStorageCredentials(
                new URL("http://localhost/pub/1")).isAdmin() == false
    }

    def "collect scheduler should restart and use new sourceFeedUrls"() {
        setup:
        def collectScheduler = new TestScheduler()
        collectScheduler.publicSourceFeedUrls = (1..100).collect {
                new URL("http://localhost/old/${it}")
            }
        collectScheduler.startup()
        def wasStarted = collectScheduler.isStarted()
        Thread.sleep 1

        when:
        def newSources = [new URL("http://example.org/new")]
        collectScheduler.publicSourceFeedUrls = newSources

        then:
        collectScheduler.sourceFeedUrls == newSources
        collectScheduler.isStarted() == wasStarted == true
    }

    class TestScheduler extends FeedCollectScheduler {
        TestScheduler() { super(null) }
        protected void collectFeed(URL feedUrl, boolean lastInBatch) {
            println "dummy collect: ${feedUrl} (lastInBatch=${lastInBatch})"
            Thread.sleep 1
        }
    }

}
