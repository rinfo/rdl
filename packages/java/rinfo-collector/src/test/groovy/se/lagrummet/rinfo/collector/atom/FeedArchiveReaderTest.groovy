package se.lagrummet.rinfo.collector.atom

import org.junit.runner.RunWith
import spock.lang.*

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.restlet.Application
import org.restlet.Component
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.Protocol
import org.restlet./*resource.*/Directory


@Speck @RunWith(Sputnik)
class FeedArchiveReaderTest {

    @Shared Component component
    @Shared def testHttpPort = 9991
    @Shared def baseUrl = "http://localhost:${testHttpPort}"

    URL feedUrl
    FeedArchiveReader reader

    def setupSpeck() {
        component = new Component()
        component.servers.add(Protocol.HTTP, testHttpPort)
        component.clients.add(Protocol.FILE)
        component.defaultHost.attach(new FeedApp())
        component.start()
    }

    def cleanupSpeck() {
        component.stop()
    }

    def setup() {
        feedUrl =  new URL("${baseUrl}/index.atom")
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

class FeedApp extends Application {
    static ROOT_URI = new File("src/test/resources/feed").toURI().toString()
    Restlet createRoot() {
        return new Directory(context, ROOT_URI)
    }
}

