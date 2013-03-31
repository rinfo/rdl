package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.commons.io.FileUtils
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.model.AtomDate

import se.lagrummet.rinfo.collector.atom.fs.FeedEntryDataIndexFSImpl


class FeedEntryDataIndexImplSpec extends Specification {

    FeedEntryDataIndex feedEntryDataIndex
    def tempDir

    def setup() {
        tempDir = File.createTempFile("complete-source-index", "",
                new File(System.getProperty("java.io.tmpdir")))
        assert tempDir.delete(); assert tempDir.mkdir()
        feedEntryDataIndex = new FeedEntryDataIndexFSImpl(tempDir)
    }

    def cleanup() {
        FileUtils.forceDelete(tempDir)
    }

    def "should persist index per feed id"() {
        given:
        def feed1 = new IRI("tag:example.org,2010:/feed/1")
        def feed2 = new IRI("tag:example.org,2010:/feed/2")
        def data = (0..9).collectEntries {
            [entryId(feed1, it), new AtomDate("2012-02-22T22:20:4${it}Z")]
        }
        assert !feedEntryDataIndex.getEntryDataForCompleteFeedId(feed1)

        when:
        feedEntryDataIndex.storeEntryDataForCompleteFeedId(feed1, data)
        then:
        data.size() == 10
        data == feedEntryDataIndex.getEntryDataForCompleteFeedId(feed1)
        and:
        !feedEntryDataIndex.getEntryDataForCompleteFeedId(feed2)

        when:
        data.remove(entryId(feed1, 0))
        and:
        feedEntryDataIndex.storeEntryDataForCompleteFeedId(feed1, data)
        then:
        data.size() == 9
        data == feedEntryDataIndex.getEntryDataForCompleteFeedId(feed1)
    }

    def entryId(feed, i) { new IRI("${feed}/entry/${i}") }

}
