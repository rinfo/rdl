package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry


class FeedArchiveReaderSpec extends Specification {

    @Shared feedApp
    @Shared baseUrl

    def setupSpec() {
        feedApp = new TestFeedApp("src/test/resources/feed")
        baseUrl = "http://localhost:${feedApp.port}"
        feedApp.start()
    }

    def cleanupSpec() {
        feedApp.stop()
    }

    URL feedUrl = new URL("${baseUrl}/index.atom")
    FeedArchiveReader reader

    def setup() {
        reader = new DummyFeeder()
    }

    def "should read feed"() {
        when:
        reader.readFeed(feedUrl)
        then:
        reader.visitedPages == [feedUrl, new URL("${baseUrl}/2.atom")]
        assert reader.wasInitialized
        assert reader.wasShutdown
    }

}


class DummyFeeder extends FeedArchiveReader {
    def visitedPages = []
    def wasInitialized = false
    def wasShutdown

    boolean processFeedPage(URL pageUrl, Feed feed) {
        visitedPages << pageUrl
        return true
    }
    void initialize() {
        super.initialize()
        wasInitialized = true
    }
    void shutdown() {
        super.shutdown()
        wasShutdown = true
    }
}


