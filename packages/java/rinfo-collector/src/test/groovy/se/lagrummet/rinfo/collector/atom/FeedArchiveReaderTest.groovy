package se.lagrummet.rinfo.collector.atom

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.restlet.Application
import org.restlet.Component
import org.restlet.Context
import org.restlet.Directory
import org.restlet.Restlet
import org.restlet.data.Protocol


class FeedArchiveReaderTest {

    private static component
    private static testHttpPort = 9991

    @BeforeClass
    static void setupClass() {
        component = new Component()
        component.servers.add(Protocol.HTTP, testHttpPort)
        component.clients.add(Protocol.FILE)
        component.defaultHost.attach(new FeedApp())
        component.start()
    }

    @AfterClass
    static void tearDownClass() {
        component.stop()
    }

    @Test
    void shouldReadFeed() {
        def baseUrl = "http://localhost:${testHttpPort}"
        def feedUrl =  new URL("${baseUrl}/index.atom")
        def reader = new DummyFeeder()
        reader.readFeed(feedUrl)
        assertEquals([feedUrl, new URL("${baseUrl}/2.atom")], reader.visitedPages)
        assertTrue reader.wasInitialized
        assertTrue reader.wasShutdown
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

