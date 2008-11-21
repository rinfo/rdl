package se.lagrummet.rinfo.base.atom

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI

import org.restlet.*
import org.restlet.data.Protocol


class FeedArchivePastToPresentReaderTest {

    private static component
    private static testHttpPort = 9991

    def baseUrl = "http://localhost:${testHttpPort}"
    def feedUrl =  new URL("${baseUrl}/index1.atom")

    @BeforeClass
    static void setupClass() {
        component = new Component()
        component.servers.add(Protocol.HTTP, testHttpPort)
        component.clients.add(Protocol.FILE)
        component.defaultHost.attach(new ChangedFeedApp())
        component.start()
    }

    @AfterClass
    static void tearDownClass() {
        component.stop()
    }

    @Test
    void shouldReadFeedInOrder() {
        def reader = new CollectReader()
        reader.readFeed(feedUrl)
        assertEquals(
                [new URL("${baseUrl}/arch1.atom"), feedUrl],
                reader.visitedPages )
        assertEquals( [
                    "http://example.org/entry/1 2000-01-01T00:00:01.000Z",
                    "http://example.org/entry/2 2000-01-01T00:00:02.000Z",
                    "http://example.org/entry/3 2000-01-01T00:00:03.000Z",
                    "http://example.org/entry/2 2000-01-01T00:01:00.000Z",
                ],
                reader.visitedEntries.collect {
                    "${it.id} ${it.updatedElement.getString()}" as String
                }
            )
    }

    @Test
    void shouldStopOnProcessedEntry() {
        def reader = new CollectReader(
                knownEntry:[id: "http://example.org/entry/3",
                             updated:"2000-01-01T00:00:03.000Z"]
            )
        reader.readFeed(feedUrl)
        assertEquals 1, reader.visitedPages.size()
        // NOTE: 2, not 1, since reader doesn't filter visiting feeds
        assertEquals 2, reader.visitedEntries.size()
    }

    @Test
    void shouldStopOnVisitedArchive() {
        def reader = new CollectReader(knownArchive:"${baseUrl}/arch1.atom")
        reader.readFeed(feedUrl)
        assertEquals 1, reader.visitedPages.size()
    }

    @Test
    void shouldPutUriDateIfNewOrYoungest() {
        def map = [:]
        def iri = new IRI("http://example.org/1")

        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                iri, new AtomDate("2000-01-01T00:01:00.000Z"))
        assertEquals new AtomDate("2000-01-01T00:01:00.000Z"), map[iri]

        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                iri, new AtomDate("2002-01-01T00:01:00.000Z"))
        assertEquals new AtomDate("2002-01-01T00:01:00.000Z"), map[iri]

        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                iri, new AtomDate("2001-01-01T00:01:00.000Z"))
        assertEquals new AtomDate("2002-01-01T00:01:00.000Z"), map[iri]

        assertEquals 1, map.size()

        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                new IRI("http://example.org/2"),
                new AtomDate("2000-01-01T00:01:00.000Z"))
        assertEquals 2, map.size()
    }

}

class CollectReader extends FeedArchivePastToPresentReader {

    def visitedPages = []
    def visitedEntries = []
    def knownEntry
    def knownArchive

    void processFeedPageInOrder(URL pageUrl, Feed feed) {
        visitedPages << pageUrl
        for (entry in feed.entries) {
            visitedEntries << entry
        }
    }

    boolean stopOnEntry(Entry entry) {
        if (!knownEntry) return false
        return (entry.id.toString() == knownEntry.id &&
                entry.updatedElement.getString() == knownEntry.updated)
    }

    boolean hasVisitedArchivePage(URL pageUrl) {
        return pageUrl.toString() == knownArchive
    }

}

class ChangedFeedApp extends Application {
    static ROOT_URI = new File(
            "src/test/resources/feed/multiplechanges").toURI().toString()
    Restlet createRoot() {
        return new Directory(context, ROOT_URI)
    }
}

