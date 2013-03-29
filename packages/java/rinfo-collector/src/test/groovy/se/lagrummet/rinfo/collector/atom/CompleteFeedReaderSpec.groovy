package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI


class CompleteFeedReaderSpec extends Specification {

    @Shared feedApp
    @Shared baseUrl

    def setupSpec() {
        feedApp = new SimpleFeedApp("src/test/resources/feed")
        baseUrl = "http://localhost:${feedApp.port}"
        feedApp.start()
    }

    def cleanupSpec() {
        feedApp.stop()
    }

    def completeUrl1 =  new URL("${baseUrl}/complete1.atom")
    def completeUrl2 =  new URL("${baseUrl}/complete2.atom")

    def "default reader should fail on complete feed"() {
        given:
        def reader = new DefaultArchiveReader()
        when:
        reader.readFeed(completeUrl1)
        then:
        thrown(UnsupportedOperationException)
        reader.entryMap.size() == 0
    }

    def "complete reader should handle complete feed"() {
        given:
        def reader = new CompleteFeedAwareReader()
        when:
        reader.readFeed(completeUrl1)
        then:
        reader.entryMap.size() == 3
        when:
        reader = new CompleteFeedAwareReader()
        reader.readFeed(completeUrl2)
        then:
        // TODO: use one deleted, one updated, one new..
        reader.entryMap.size() == 2
    }

}


class DefaultArchiveReader extends FeedArchivePastToPresentReader {
    def entryMap = [:]
    @Override
    boolean processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deleteds) {
        deleteds.keySet().each {
            entryMap.remove it
        }
        effectiveEntries.each {
            entryMap[it.id] = it.updatedElement.value
        }
        return true
    }
    @Override
    boolean stopOnEntry(Entry entry) {
        return entryMap[entry.id] == entry.updatedElement.value
    }
}


class CompleteFeedAwareReader extends DefaultArchiveReader {
    FeedEntryDataIndex feedEntryDataIndex = new FeedEntryDataIndexMemImpl()
}
