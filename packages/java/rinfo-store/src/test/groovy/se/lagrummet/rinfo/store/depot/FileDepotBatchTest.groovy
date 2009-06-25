package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.*


class FileDepotBatchTest {

    static FileDepot fileDepot

    @BeforeClass
    static void setupClass() {

        fileDepot = (FileDepot) DepotUtil.depotFromConfig(
                "src/test/resources/rinfo-depot.properties");
    }

    @Test
    void shouldAddAndRetrieve() {
        def entryId = "/publ/1901/100"
        def entry = fileDepot.getEntry(entryId)
        def batch = fileDepot.makeEntryBatch()
        assertEquals batch.size(), 0
        batch << entry
        assertEquals batch.size(), 1
        batch << entry
        assertEquals batch.size(), 1
        batch << fileDepot.getEntry("/publ/1901/100/revisions/1902/200")
        assertEquals batch.size(), 2
    }

    @Test(expected=NullPointerException)
    void shouldFailOnNull() {
        def batch = fileDepot.makeEntryBatch()
        batch << null
    }

    @Test
    void shouldContainAddedEntry() {
        def entryId = "/publ/1901/100"
        def entry = fileDepot.getEntry(entryId)
        assertNotNull entry
        def batch = fileDepot.makeEntryBatch()
        batch << entry
        assertTrue batch.contains(fileDepot.getEntry(entryId))
    }

    @Test
    void shouldNotContainNotAddedEntry() {
        def entryId = "/publ/1901/100"
        def entry = fileDepot.getEntry(entryId)
        def batch = fileDepot.makeEntryBatch()
        assertFalse batch.contains(fileDepot.getEntry(entryId))
    }

}
