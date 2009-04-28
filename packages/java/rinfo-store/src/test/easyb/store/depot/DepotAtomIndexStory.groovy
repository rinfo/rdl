import org.apache.abdera.ext.history.FeedPagingHelper as FPH
import se.lagrummet.rinfo.store.depot.SourceContent


import se.lagrummet.rinfo.store.depot.FileDepotTempBase as DepotTestHelper
DepotTestHelper.setupClass() // TODO:IMPROVE: crufty reuse of junit helper..
depot = DepotTestHelper.fileDepot
depot.atomizer.feedBatchSize = 2

scenario "a pre-filled depot is indexed", {

    given "an initialized depot"

    when "an index has been built", {
        depot.generateIndex()
    }

    then "a subscription feed should be available", {
        current = depot.atomizer.getFeed(depot.subscriptionPath)
        current.shouldNotBe null
    }

    and "archives should be chained and cut by batch size", {
        entryIds = []
        storeIds = { entryIds += it.entries.collect { it.id.toString() } }

        current.entries.size().shouldBe 1
        FPH.isArchive(current).shouldBe false
        def currentLink = current.selfLink.href
        storeIds current

        def prev1 = depot.atomizer.getPrevArchiveAsFeed(current)
        FPH.isArchive(prev1).shouldBe true
        FPH.getCurrent(prev1).shouldBe currentLink
        FPH.getNextArchive(prev1).shouldBe null
        prev1.entries.size().shouldBe 2
        storeIds prev1

        def prev2 = depot.atomizer.getPrevArchiveAsFeed(prev1)
        FPH.isArchive(prev2).shouldBe true
        FPH.getCurrent(prev2).shouldBe currentLink
        FPH.getNextArchive(prev2).shouldBe depot.atomizer.uriPathFromFeed(prev1)
        FPH.getPreviousArchive(prev2).shouldBe null
        prev2.entries.size().shouldBe 2
        storeIds prev2
    }

    and "all items should have been indexed", {
        entryIds.shouldBe([
            "http://example.org/publ/1901/locked",
            "http://example.org/dataset",
            "http://example.org/publ/1901/100/revisions/1902/200",
            "http://example.org/publ/1901:101",
            "http://example.org/publ/1901/100",
        ])
    }

}

scenario "an existing depot is filled with additional entries", {

    when "a batch is indexed", {
        def batch = depot.makeEntryBatch()

        def newEntryUri = new URI("http://example.org/publ/NEW/1")
        def createTime = new Date()
        batch << depot.createEntry(newEntryUri, createTime, [
                    new SourceContent(DepotTestHelper.exampleEntryFile(
                            "content-en.pdf"), "application/pdf", "en"),
                    new SourceContent(DepotTestHelper.exampleEntryFile(
                            "content.rdf"), "application/rdf+xml")
                ])
        depot.indexEntries(batch)
    }

    then "entries should have been inserted in subscription feed", {
        current = depot.atomizer.getFeed(depot.subscriptionPath)
        current.entries.size().shouldBe 2
    }

    and "archives should be identified by latest date in batch", {
    }

}

DepotTestHelper.tearDownClass()

