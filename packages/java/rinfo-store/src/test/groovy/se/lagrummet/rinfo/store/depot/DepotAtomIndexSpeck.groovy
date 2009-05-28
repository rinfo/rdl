package se.lagrummet.rinfo.store.depot

import org.junit.runner.RunWith
import spock.lang.*
import org.apache.abdera.ext.history.FeedPagingHelper as FPH


@Speck @RunWith(Sputnik)
class DepotAtomIndexSpeck extends FileDepotTempBase {

    @Shared depot

    def setupSpeck() {
        setupClass()
        depot = fileDepot
        depot.atomizer.feedBatchSize = 2
    }

    def cleanupSpeck() { tearDownClass() }

    def "a pre-filled depot is indexed"() {

        when: "an index has been built"
        depot.generateIndex()

        then: "a subscription feed should be available"
        def current = depot.atomizer.getFeed(depot.subscriptionPath)
        current != null

        and: "archives should be chained and cut by batch size"
        def entryIds = []
        def storeIds = { entryIds += it.entries.collect { it.id as String } }

        current.entries.size() == 1
        FPH.isArchive(current) == false
        def currentLink = current.selfLink.href
        storeIds current

        def prev1 = depot.atomizer.getPrevArchiveAsFeed(current)
        FPH.isArchive(prev1) == true
        FPH.getCurrent(prev1) == currentLink
        FPH.getNextArchive(prev1) == null
        prev1.entries.size() == 2
        storeIds prev1

        def prev2 = depot.atomizer.getPrevArchiveAsFeed(prev1)
        FPH.isArchive(prev2) == true
        FPH.getCurrent(prev2) == currentLink
        FPH.getNextArchive(prev2) as String == depot.atomizer.uriPathFromFeed(prev1)
        FPH.getPreviousArchive(prev2) == null
        prev2.entries.size() == 2
        storeIds prev2

        and: "all items should have been indexed"
        entryIds == [
            "http://example.org/publ/1901/locked",
            "http://example.org/dataset",
            "http://example.org/publ/1901/100/revisions/1902/200",
            "http://example.org/publ/1901:101",
            "http://example.org/publ/1901/100",
        ]
    }

    def "an existing depot is filled with additional entries"() {

        when: "a batch is indexed"
        def batch = depot.makeEntryBatch()

        def newEntryUri = new URI("http://example.org/publ/NEW/1")
        def createTime = new Date()
        batch << depot.createEntry(newEntryUri, createTime, [
                    new SourceContent(exampleEntryFile(
                            "content-en.pdf"), "application/pdf", "en"),
                    new SourceContent(exampleEntryFile(
                            "content.rdf"), "application/rdf+xml")
                ])
        depot.indexEntries(batch)

        then: "entries should have been inserted in subscription feed"
        def current = depot.atomizer.getFeed(depot.subscriptionPath)
        current.entries.size() == 2

        and: "archives should be identified by exclusive youngest entry date"
        def datePath = DatePathUtil.toFeedArchivePath(current.entries[-1].updated)
        def prev = depot.atomizer.getPrevArchiveAsFeed(current)
        prev.selfLink.href =~ "${depot.feedPath}/${datePath}\$"

    }

}
