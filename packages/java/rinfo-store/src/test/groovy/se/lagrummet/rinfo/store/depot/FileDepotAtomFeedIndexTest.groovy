package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*

import org.apache.abdera.Abdera
import org.apache.abdera.model.AtomDate


class FileDepotAtomFeedIndexTest extends FileDepotTempBase {

    @Before
    void setup() {
        fileDepot.atomizer.feedBatchSize = 2
    }

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
