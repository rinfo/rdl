package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI


class FeedArchivePastToPresentReaderSpec extends Specification {

    @Shared feedApp
    @Shared baseUrl

    def setupSpec() {
        feedApp = new SimpleFeedApp("src/test/resources/feed/multiplechanges")
        baseUrl = "http://localhost:${feedApp.port}"
        feedApp.start()
    }

    def cleanupSpec() {
        feedApp.stop()
    }

    def feedUrl =  new URL("${baseUrl}/index1.atom")

    def "should read feed in order"() {
        setup:
        def reader = new CollectReader()
        when:
        reader.readFeed(feedUrl)
        then:
        reader.visitedPages == [new URL("${baseUrl}/arch1.atom"), feedUrl]
        and:
        reader.effectiveEntries.size() == 3
        reader.entryRow(0) == "<http://example.org/entry:1> @ 2000-01-01T00:00:01.000Z"
        reader.entryRow(1) == "<http://example.org/entry:3> @ 2000-01-01T00:00:03.000Z"
        reader.entryRow(2) == "<http://example.org/entry:2> @ 2000-01-01T00:01:00.000Z"
    }

    def "should stop on processed entry"() {
        when:
        def reader = new CollectReader(
                knownEntry:[id: "http://example.org/entry:3",
                             updated:"2000-01-01T00:00:03.000Z"])
        reader.readFeed(feedUrl)
        then:
        reader.visitedPages.size() == 1
        reader.effectiveEntries.size() == 1
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
        reader.effectiveEntries.size() == 2
    }

    def "should note deletes younger than updated"() {
        setup:
        def reader = new CollectReader()
        when:
        reader.readFeed(new URL("${baseUrl}/updated_deleted.atom"))
        then:
        reader.deleteds.size() == 1
        reader.deletedRow(0) == "<http://example.org/doc/2> @ 2000-01-01T00:00:03.000Z"
        and:
        reader.effectiveEntries.size() == 1
        reader.entryRow(0) == "<http://example.org/doc/1> @ 2000-01-01T00:00:01.000Z"
    }

    def "an older delete mustn't supress a younger updated"() {
        setup:
        def reader = new CollectReader()
        when:
        reader.readFeed(new URL("${baseUrl}/deleted_updated.atom"))
        then:
        reader.deleteds.size() == 0
        and:
        reader.effectiveEntries.size() == 1
        reader.entryRow(0) == "<http://example.org/doc/1> @ 2000-01-01T00:00:04.000Z"
    }

    // TODO: more high-level, more meaningful specs(s) than "put uri if.." below
    //def "should report resurrected entry"() {
    //}

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

    def "should handle charsets properly"() {
        given:
        def reader = new CollectReader()
        expect:
        reader.readFeed(new URL("${baseUrl}/windows-1252.atom"))
        reader.effectiveEntries.size() == 1
    }

}

class CollectReader extends FeedArchivePastToPresentReader {

    def visitedPages = []
    def effectiveEntries = []
    def deleteds = []
    def knownEntry
    def knownArchive

    @Override
    void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deleteds) {
        visitedPages << pageUrl
        for (entry in effectiveEntries) this.effectiveEntries << entry
        for (deleted in deleteds.entrySet()) this.deleteds << deleted
    }

    @Override
    boolean stopOnEntry(Entry entry) {
        if (!knownEntry) return false
        return (entry.id.toString() == knownEntry.id &&
                entry.updatedElement.getString() == knownEntry.updated)
    }

    @Override
    boolean hasVisitedArchivePage(URL pageUrl) {
        return pageUrl.toString() == knownArchive
    }

    String entryRow(int index) {
        def entry = effectiveEntries[index]
        return "<${entry.id}> @ ${entry.updatedElement.string}" as String
    }

    String deletedRow(int index) {
        def deleted = deleteds[index]
        return "<${deleted.key}> @ ${deleted.value}" as String
    }

}
