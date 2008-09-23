package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*


class FileDepotAtomIndexTest extends FileDepotTempBase {

    @Before
    void setup() {
        fileDepot.atomizer.feedBatchSize = 2
    }

    @Test
    void shouldGenerateAtomEntry() {
        def entry = fileDepot.getEntry("/publ/1901/100")
        assertEquals 0, entry.findContents(fileDepot.pathProcessor.
                hintForMediaType("application/atom+xml;type=entry")).size()
        fileDepot.onEntryModified(entry)
        def atomContent = entry.findContents("application/atom+xml;type=entry")[0]
        assert atomContent.file.isFile()
        // TODO: specify content, alternatives, enclosures, size, md5(?)
    }

    // TODO: shouldGenerateAtomEntryWhenIndexingNewEntry / or WhenCreating?
    // TODO: shouldGenerateAtomEntryWhenModified

    @Test
    void shouldGenerateIndex() {
        fileDepot.generateIndex()
        def feed = fileDepot.atomizer.getFeed(fileDepot.subscriptionPath)
        assertNotNull feed
        // TODO: list feeds.., count entries
        // - writesFeedByLatestDateInBatch
        // - confirmDoesFullIndexing
        // - archiveFeedsChain
    }

    // TODO: shouldInsertInSubscriptionFeed
    // TODO: shouldCutOffSubscriptionToArchiveAtBatchSize

    /* FIXME: testIndexBatch
    static def suiteBatch
    suiteBatch = fileDepot.makeEntryBatch()
    fileDepot.indexEntries(suiteBatch)
    */

}
