package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI


class FeedOfFeedsReaderSpec extends Specification {

    @Shared feedApp
    @Shared baseUrl

    def setupSpec() {
        feedApp = new SimpleFeedApp("src/test/resources/feed-of-feeds")
        baseUrl = "http://localhost:${feedApp.port}"
        feedApp.start()
    }

    def cleanupSpec() {
        feedApp.stop()
    }


    def "should handle feed of feeds"() {
        given:
        def indexUrl1 =  new URL("${baseUrl}/index-1.atom")
        def indexUrl2 =  new URL("${baseUrl}/index-2.atom")
        def feedEntryDataIndex = new FeedEntryDataIndexMemImpl()
        def entryMap = [:]
        def reader = new FeedOfFeedsReader(feedEntryDataIndex, entryMap)
        when:
        reader.readFeed(indexUrl1)
        then:
        reader.pageVisits.size() == 2
        entryMap.size() == 3
        when:
        reader = new FeedOfFeedsReader(feedEntryDataIndex, entryMap)
        reader.readFeed(indexUrl2)
        then:
        reader.pageVisits.size() == 1
        entryMap.size() == 4
    }

}


class FeedOfFeedsReader extends FeedArchivePastToPresentReader {

    FeedEntryDataIndex feedEntryDataIndex
    def entryMap
    def pageVisits = []

    FeedOfFeedsReader(feedEntryDataIndex, entryMap) {
        this.feedEntryDataIndex = feedEntryDataIndex
        this.entryMap = entryMap
    }

    @Override
    void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deleteds) {
        pageVisits << pageUrl
        deleteds.keySet().each {
            entryMap.remove it
        }
        effectiveEntries.each {
            entryMap[it.id] = it.updatedElement.value
        }
    }

    @Override
    boolean stopOnEntry(Entry entry) {
        return entryMap[entry.id] == entry.updatedElement.value
    }

}
