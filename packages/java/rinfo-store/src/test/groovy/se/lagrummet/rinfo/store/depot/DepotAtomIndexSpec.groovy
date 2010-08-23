package se.lagrummet.rinfo.store.depot

import spock.lang.*
import org.apache.abdera.ext.history.FeedPagingHelper as FPH


class DepotAtomIndexSpec extends Specification {

    @Shared Depot depot
    @Shared Atomizer atomizer
    @Shared def tdu = new TempDepotUtil()

    def setupSpec() {
        depot = tdu.createTempDepot()
        atomizer = depot.atomizer
        atomizer.feedBatchSize = 2
    }

    def cleanupSpec() {
        tdu.deleteTempDepot()
    }

    def "should generate atom entry"() {
        when:
        def entry = depot.getEntry("/publ/1901/100")
        then:
        entry.findContents("application/atom+xml;type=entry").size() == 0

        when:
        new AtomIndexer(depot.atomizer, depot.backend).indexEntry(entry)
        def atomContent = entry.findContents("application/atom+xml;type=entry")[0]
        then:
        assert atomContent.file.isFile()
        // TODO: specify content, alternatives, enclosures, size, md5(?)
    }

    // TODO: shouldGenerateAtomEntryWhenIndexingNewEntry / or WhenCreating?
    // TODO: shouldGenerateAtomEntryWhenModified

    def "a pre-filled depot is indexed"() {

        when: "an index has been built"
        depot.generateIndex()
        def indexer = new AtomIndexer(depot.atomizer, depot.backend)

        then: "a subscription feed should be available"
        def current = indexer.getFeed(atomizer.subscriptionPath)
        current != null

        and: "archives should be chained and cut by batch size"
        def entryIds = []
        def storeIds = { entryIds += it.entries.collect { it.id as String } }

        current.entries.size() == 2
        FPH.isArchive(current) == false
        def currentLink = current.selfLink.href
        storeIds current

        def prev1 = indexer.getPrevArchiveAsFeed(current)
        FPH.isArchive(prev1) == true
        FPH.getCurrent(prev1) == currentLink
        FPH.getNextArchive(prev1) == null
        prev1.entries.size() == 2
        storeIds prev1

        def prev2 = indexer.getPrevArchiveAsFeed(prev1)
        FPH.isArchive(prev2) == true
        FPH.getCurrent(prev2) == currentLink
        FPH.getNextArchive(prev2) as String == atomizer.uriPathFromFeed(prev1)
        FPH.getPreviousArchive(prev2) == null
        prev2.entries.size() == 2
        storeIds prev2

        and: "all items should have been indexed"
        entryIds == [
            "http://example.org/publ/1901/locked",
            "http://example.org/publ/1901/0",
            "http://example.org/dataset",
            "http://example.org/publ/1901/100/revisions/1902/200",
            "http://example.org/publ/1901:101",
            "http://example.org/publ/1901/100",
        ]
    }

    def "an existing depot is filled with additional entries"() {

        when: "a batch is indexed"
        def session = depot.openSession()
        def indexer = session.atomIndexer

        def newEntryUri = new URI("http://example.org/publ/NEW/1")
        def createTime = new Date()
        session.createEntry(newEntryUri, createTime, [
                    new SourceContent(tdu.exampleEntryFile(
                            "content-en.pdf"), "application/pdf", "en"),
                    new SourceContent(tdu.exampleEntryFile(
                            "content.rdf"), "application/rdf+xml")
                ])
        session.close()

        then: "entry should have been inserted in new subscription feed"
        def current = indexer.getFeed(atomizer.subscriptionPath)
        current.entries.size() == 1
        def atomEntry = current.entries[0]
        atomEntry.id.toURI() == newEntryUri
        atomEntry.updated == createTime

        and: "archives should be identified by exclusive youngest entry date"
        def datePath = DatePathUtil.toFeedArchivePath(current.entries[-1].updated)
        def prev = indexer.getPrevArchiveAsFeed(current)
        prev.selfLink.href =~ "${depot.atomizer.feedPath}/${datePath}\$"

    }

}
