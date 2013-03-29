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

    @Shared fedIndex
    @Shared entryMap

    def setup() {
        fedIndex = new FeedEntryDataIndexMemImpl()
        entryMap = [:]
    }

    def indexUrl1 =  new URL("${baseUrl}/index-1.atom")
    def indexUrl2 =  new URL("${baseUrl}/index-2.atom")

    def "should handle feed of feeds"() {
        given:
        def reader = new FeedOfFeedsReader(fedIndex, entryMap)
        when:
        reader.readFeed(indexUrl1)
        then:
        reader.pageVisits.size() == 2
        entryMap.size() == 3
        when:
        reader = new FeedOfFeedsReader(fedIndex, entryMap)
        reader.readFeed(indexUrl2)
        then:
        reader.pageVisits.size() == 1
        entryMap.size() == 4
    }

    def "should store collected feed date in index"() {
        given:
        def reader = new FeedOfFeedsReader(fedIndex, entryMap)
        when:
        reader.readFeed(indexUrl1)
        and:
        reader = new FeedOfFeedsReader(fedIndex, entryMap)
        reader.failOnUrl = failOnUrl? new URL(failOnUrl) : null
        reader.readFeed(indexUrl2)
        then:
        def data = fedIndex.getEntryDataForCompleteFeedId(
                new IRI("tag:sfs.regeringen.se,2013:rinfo:feed:index"))
        data[new IRI("tag:sfs.regeringen.se,2013:rinfo:feed:1686")] ==
                new AtomDate(storedFeedUpdated)
        where:
        failOnUrl                   | storedFeedUpdated
        null                        | "2012-12-31T22:33:45Z"
        "${baseUrl}/1686-2.atom"    | "2012-12-31T22:33:44Z"
    }

}


class FeedOfFeedsReader extends FeedArchivePastToPresentReader {

    FeedEntryDataIndex feedEntryDataIndex
    def entryMap
    def pageVisits = []
    def failOnUrl = null

    FeedOfFeedsReader(feedEntryDataIndex, entryMap) {
        this.feedEntryDataIndex = feedEntryDataIndex
        this.entryMap = entryMap
    }

    @Override
    boolean processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deleteds) {
        pageVisits << pageUrl
        deleteds.keySet().each {
            entryMap.remove it
        }
        effectiveEntries.each {
            entryMap[it.id] = it.updatedElement.value
        }
        return pageUrl != failOnUrl
    }

    @Override
    boolean stopOnEntry(Entry entry) {
        return entryMap[entry.id] == entry.updatedElement.value
    }

}
