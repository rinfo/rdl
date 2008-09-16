package se.lagrummet.rinfo.base.atom

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
        component.defaultHost.attach(new FileApp())
        component.start()
    }

    @AfterClass
    static void tearDownClass() {
        component.stop()
    }

    @Test
    void shouldReadFeed() {
        def feedUrl =  new URL("http://localhost:${testHttpPort}/index.atom")
        def reader = new DummyFeeder()
        reader.readFeed(feedUrl)
        assertEquals 2, reader.visitedPages
    }

}

class DummyFeeder extends FeedArchiveReader {

    int visitedPages

    boolean processFeedPage(URL pageUrl, Feed feed) {
        visitedPages += 1
        return true
    }

}

class FileApp extends Application {
    static ROOT_URI = new File("src/test/resources/feed").toURI().toString()
    Restlet createRoot() {
        return new Directory(context, ROOT_URI)
    }
}

