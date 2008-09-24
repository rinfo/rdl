package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.*


class FileDepotTransactionTest extends FileDepotTempBase {

    @Test
    void shouldLockEntry() {
        def entryId = "/publ/1901/100"
        def entry = fileDepot.getEntry(entryId)
        entry.lock()
        assertTrue entry.isLocked()
        try {
            fileDepot.getEntry(entryId)
            fail("Expected entry to be locked!")
        } catch (LockedDepotEntryException e) {
        }
        entry.unlock()
        assertFalse entry.isLocked()
        entry = fileDepot.getEntry(entryId)
        assertFalse entry.isLocked()
    }

    @Test
    void shouldOperateViaBatch() {
        /* TODO:? Really?
        def batch = depot.makeEntryBatch()
        batch.create
        assertTrue locked
        batch.update
        ...
        batch.delete
        ...
        batch.commit()
        assert hasBeenIndexed
        */
    }

}
