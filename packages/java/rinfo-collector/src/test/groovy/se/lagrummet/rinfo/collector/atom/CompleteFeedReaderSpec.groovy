package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI


class CompleteFeedReaderSpec extends Specification {

    @Shared feedApp
    @Shared baseUrl

    def setupSpeck() {
        feedApp = new TestFeedApp("src/test/resources/feed")
        baseUrl = "http://localhost:${feedApp.port}"
        feedApp.start()
    }

    def cleanupSpeck() {
        feedApp.stop()
    }

    def feedUrl =  new URL("${baseUrl}/complete1.atom")
    // "complete-source-index"

    def "default reader should fail on complete feed"() {
        setup:
        def reader = new FeedArchivePastToPresentReader() {
            @Override
            void processFeedPageInOrder(URL pageUrl, Feed feed,
                    List<Entry> effectiveEntries, Map<IRI, AtomDate> deleteds) {
                ;
            }
            @Override
            boolean stopOnEntry(Entry entry) {
                return false
            }
        }
        when:
        reader.readFeed(feedUrl)
        then:
        thrown(UnsupportedOperationException)
    }

}
