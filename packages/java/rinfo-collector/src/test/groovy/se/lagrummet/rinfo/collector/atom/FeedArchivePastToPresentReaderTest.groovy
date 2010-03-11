package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI

import org.restlet.*
import org.restlet.data.Protocol
import org.restlet./*resource.*/Directory


class FeedArchivePastToPresentReaderTest extends Specification {

    @Shared component
    @Shared testHttpPort = 9991

    def baseUrl = "http://localhost:${testHttpPort}"
    def feedUrl =  new URL("${baseUrl}/index1.atom")

    def setupSpeck() {
        component = new Component()
        component.servers.add(Protocol.HTTP, testHttpPort)
        component.clients.add(Protocol.FILE)
        component.defaultHost.attach(new ChangedFeedApp())
        component.start()
    }

    def cleanupSpeck() {
        component.stop()
    }

    def "should read feed in order"() {
        when:
        def reader = new CollectReader()
        reader.readFeed(feedUrl)
        then:
        reader.visitedPages == [new URL("${baseUrl}/arch1.atom"), feedUrl]
        and:
        reader.visitedEntries.collect {
            "${it.id} ${it.updatedElement.getString()}" as String
        } == [
            "http://example.org/entry:1 2000-01-01T00:00:01.000Z",
            // not: "http://example.org/entry:2 2000-01-01T00:00:02.000Z",
            "http://example.org/entry:3 2000-01-01T00:00:03.000Z",
            "http://example.org/entry:2 2000-01-01T00:01:00.000Z",
        ]
    }

    def "should stop on processed entry"() {
        when:
        def reader = new CollectReader(
                knownEntry:[id: "http://example.org/entry:3",
                             updated:"2000-01-01T00:00:03.000Z"])
        reader.readFeed(feedUrl)
        then:
        reader.visitedPages.size() == 1
        reader.visitedEntries.size() == 1
    }

    def "should stop on visited archive"() {
        when:
        def reader = new CollectReader(knownArchive:"${baseUrl}/arch1.atom")
        reader.readFeed(feedUrl)
        then:
        reader.visitedPages.size() == 1
    }

    def "should collect entries with same timestamp as processed entry"() {
        when:
        def reader = new CollectReader(
                knownEntry:[id: "http://example.org/entry:1",
                             updated:"2000-01-01T00:00:00.000Z"])
        reader.readFeed(new URL("${baseUrl}/entries_with_updated_dups.atom"))
        then:
        reader.visitedEntries.size() == 2
    }

    /* TODO: high-level meaningful specs(s) related to "putUriDateIfNewOrYoungest":
        - shouldReportResurrectedEntry:
            an older delete mustn't supress a younged updated
            - given continuous feed events:
                - Entry(id="123", published=1)
                - Entry(id="123", updated=2)
                - Entry(id="123", deleted=3)
                - Entry(id="123", updated=4)
            - should report:
                - Entry(id="123", deleted=3)
                - Entry(id="123", updated=4)

        * TODO: write runnable specs like the above for most tests!
            - robotframework or BDD unit-tests?
    */

    def "should put uri if new or date is youngest"() {
        setup:
        def map = [:]
        def iri = new IRI("http://example.org/1")

        when:
        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                iri, new AtomDate("2000-01-01T00:01:00.000Z"))
        then:
        map[iri] == new AtomDate("2000-01-01T00:01:00.000Z")

        when:
        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                iri, new AtomDate("2002-01-01T00:01:00.000Z"))
        then:
        map[iri] == new AtomDate("2002-01-01T00:01:00.000Z")

        when:
        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                iri, new AtomDate("2001-01-01T00:01:00.000Z"))
        then:
        map[iri] == new AtomDate("2002-01-01T00:01:00.000Z")

        and:
        1 == map.size()

        when:
        FeedArchivePastToPresentReader.putUriDateIfNewOrYoungest(map,
                new IRI("http://example.org/2"),
                new AtomDate("2000-01-01T00:01:00.000Z"))
        then:
        map.size() == 2
    }

}

class CollectReader extends FeedArchivePastToPresentReader {

    def visitedPages = []
    def visitedEntries = []
    def knownEntry
    def knownArchive

    @Override
    void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deleteds) {
        visitedPages << pageUrl
        for (entry in effectiveEntries) {
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

