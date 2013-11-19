package se.lagrummet.rinfo.main.storage

import spock.lang.*


class FeedCollectSchedulerSpec extends Specification {

    def "collect scheduler should add adminFeedUrl to sourceFeedUrls"() {
        setup:
        def collectScheduler = new FeedCollectScheduler(null)
        when:
        collectScheduler.adminFeedUrl = adminUrl
        collectScheduler.sources = sourceUrls.collect {
                new CollectorSource(it, it.toURL()) }
        then:
        def adminUrls = adminUrl? [adminUrl.toURI()] : []
        collectScheduler.sourceFeedUrls == adminUrls + sourceUrls
        where:
        adminUrl << [
            null,
            new URL("http://localhost/admin"),
            new URL("http://localhost/admin"),
            null,
        ]
        sourceUrls << [
            [new URI("http://localhost/pub/1"), new URI("http://localhost/pub/2")],
            [new URI("http://localhost/pub/1"), new URI("http://localhost/pub/2")],
            [],
            [],
        ]
    }

    def "collect scheduler should authen adminFeedUrl in credentials"() {
        setup:
        def collectScheduler = new FeedCollectScheduler(null)
        def adminUrl = new URL("http://localhost/admin")
        def otherUrl = new URL("http://localhost/pub/1")
        def unknownUrl = new URL("http://localhost/pub/2")
        when:
        collectScheduler.adminFeedUrl = adminUrl
        collectScheduler.sources = [new CollectorSource(null, otherUrl)]
        then:
        collectScheduler.getStorageCredentials(adminUrl).isAdmin() == true
        collectScheduler.getStorageCredentials(otherUrl).isAdmin() == false
        collectScheduler.getStorageCredentials(unknownUrl) == null
    }

    def "collect scheduler should restart and use new sourceFeedUrls"() {
        setup:
        def collectScheduler = new TestScheduler()
        collectScheduler.sources = (1..100).collect {
                def url = new URL("http://localhost/old/${it}")
                new CollectorSource(url.toURI(), url)
            }
        collectScheduler.startup()
        def wasStarted = collectScheduler.isStarted()
        Thread.sleep 1

        when:
        def newSources = [
                new CollectorSource(new URI("tag:example.org,2011:feed"),
                        new URL("http://example.org/new"))
            ]
        collectScheduler.sources = newSources

        then:
        collectScheduler.sourceFeedUrls == newSources.collect { it.currentFeed.toURI() }
        collectScheduler.isStarted() == wasStarted == true
    }

    def "collect scheduler should use all listed sources"() {
        // This testcase is dependent on *.testfeed.lagrummet.se has an active vritual host. the two hosts points to the same IP addresses
        setup:
        def collectScheduler = new TestScheduler()
        when:
        collectScheduler.sources = [
                new CollectorSource(new URI("tag:regeringen.se,2009:rinfo:dataset:sfs"),new URL("http://sfs.testfeed.lagrummet.se/feed/current.atom")),
                new CollectorSource(new URI("tag:regeringen.se,2009:rinfo:dataset:prop"),new URL("http://prop.testfeed.lagrummet.se/feed/current.atom"))
        ]
        then:
        collectScheduler.sourceFeedUrls.size() == 2
    }

    class TestScheduler extends FeedCollectScheduler {
        TestScheduler() { super(null) }
        protected void collectFeed(URL feedUrl, boolean lastInBatch) {
            println "dummy collect: ${feedUrl} (lastInBatch=${lastInBatch})"
            Thread.sleep 1
        }
    }

}
