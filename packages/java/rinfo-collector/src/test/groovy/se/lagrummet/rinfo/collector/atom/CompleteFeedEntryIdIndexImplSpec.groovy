package se.lagrummet.rinfo.collector.atom

import spock.lang.*

import org.apache.commons.io.FileUtils
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.collector.atom.fs.CompleteFeedEntryIdIndexFSImpl


class CompleteFeedEntryIdIndexImplSpec extends Specification {

    CompleteFeedEntryIdIndex completeFeedEntryIdIndex
    def tempDir

    def setup() {
        tempDir = File.createTempFile("complete-source-index", "",
                new File(System.getProperty("java.io.tmpdir")))
        assert tempDir.delete(); assert tempDir.mkdir()
        completeFeedEntryIdIndex = new CompleteFeedEntryIdIndexFSImpl(tempDir)
    }

    def cleanup() {
        FileUtils.forceDelete(tempDir)
    }

    def "should persist index per feed id"() {
        given:
        def feed1 = new IRI("tag:example.org,2010:/feed/1")
        def feed2 = new IRI("tag:example.org,2010:/feed/2")
        def entries1 = (0..9).collect { entryId(feed1, it) } as Set
        assert !completeFeedEntryIdIndex.getEntryIdsForCompleteFeedId(feed1)

        when:
        completeFeedEntryIdIndex.storeEntryIdsForCompleteFeedId(feed1, entries1)
        then:
        entries1.size() == 10
        entries1 == completeFeedEntryIdIndex.getEntryIdsForCompleteFeedId(feed1)
        and:
        !completeFeedEntryIdIndex.getEntryIdsForCompleteFeedId(feed2)

        when:
        entries1.remove(entryId(feed1, 0))
        and:
        completeFeedEntryIdIndex.storeEntryIdsForCompleteFeedId(feed1, entries1)
        then:
        entries1.size() == 9
        entries1 == completeFeedEntryIdIndex.getEntryIdsForCompleteFeedId(feed1)
    }

    def entryId(feed, i) { new IRI("${feed}/entry/${i}") }

}
