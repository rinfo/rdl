package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.*


class FileDepotTransactionTest extends FileDepotTempBase {

    static final EX_ID_1 = "/publ/1901/100"
    static final BROKEN_ID_1 = new URI("http://example.org/publ/NEW/broken_1")
    static final NEW_ID_1 = new URI("http://example.org/publ/NEW/rollback_1")
    static final UPD_ID_1 = new URI("http://example.org/publ/UPD/rollback_1")


    @Test
    void shouldLockEntry() {
        def entry = fileDepot.getEntry(EX_ID_1)
        entry.lock()
        assertTrue entry.isLocked()
        try {
            fileDepot.getEntry(EX_ID_1)
            fail("Expected entry to be locked!")
        } catch (LockedDepotEntryException e) {
        }
        entry.unlock()
        assertFalse entry.isLocked()
        entry = fileDepot.getEntry(EX_ID_1)
        assertFalse entry.isLocked()
    }


    @Test
    void shouldLeaveLockedOnBadContent() {
        try {
            fileDepot.createEntry(BROKEN_ID_1, new Date(),
                    [ new SourceContent(((InputStream)null), null, null) ]
                )
            fail("Should fail with nullpointer.")
        } catch (NullPointerException e) {
        }
        try {
            fileDepot.getEntry(BROKEN_ID_1)
            fail("Should fail on locked.")
        } catch (LockedDepotEntryException e) {
        }
        def brokenEntry = fileDepot.getUncheckedDepotEntry(
                BROKEN_ID_1.getPath())
        assertTrue brokenEntry.isLocked()
    }


    @Test
    void shouldWipeOnRollbackNew() {
        def entry = fileDepot.createEntry(NEW_ID_1, new Date(),
                [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
                    new SourceContent(exampleEntryFile("content.rdf"),
                            "application/rdf+xml")
                ],
            )
        assertNotNull fileDepot.getEntry(NEW_ID_1)
        entry.lock()
        assertTrue entry.rollback()
        assertNull fileDepot.getEntry(NEW_ID_1)
        // TODO: IllegalStateException on any futher entry ops..
    }


    @Test
    void shouldUnupdateOnRollbackUpdated() {
        def entry = fileDepot.createEntry(UPD_ID_1, new Date(), [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")
                ], [
                    new SourceContent(exampleFile("icon.png"),
                            null, null, "images/icon.png"),
                ]
            )

        Thread.sleep(900)
        def updateTime = new Date()
        entry = fileDepot.getEntry(UPD_ID_1)
        entry.update(updateTime, [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                                "application/pdf", "en"),
                    new SourceContent(exampleEntryFile("content.rdf"),
                            "application/rdf+xml")
                ], [
                    new SourceContent(exampleFile("icon.png"),
                            null, null, "icon.png"),
                    new SourceContent(exampleFile("icon.png"),
                            null, null, "images/icon.png"),
                ]
            )
        assertEquals updateTime, entry.updated
        assertEquals 3, entry.findContents().size()
        assertEquals 2, entry.findEnclosures().size()
        def storedModified = entry.lastModified()
        entry.getMetaFile("TEST_META_FILE").setText("TEST")
        assertTrue entry.getMetaFile("TEST_META_FILE").exists()

        Thread.sleep(900)
        entry = fileDepot.getEntry(UPD_ID_1)
        entry.update(new Date(), [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")
                ], [
                    new SourceContent(exampleFile("icon.png"),
                            null, null, "images/icon.png"),
                ]
            )
        assertEquals 2, entry.findContents().size()
        assertEquals 1, entry.findEnclosures().size()
        assertTrue updateTime < entry.updated
        assertTrue storedModified < entry.lastModified()
        assertFalse entry.getMetaFile("TEST_META_FILE").exists()

        Thread.sleep(900)
        entry.lock()
        assertTrue entry.rollback()
        entry.unlock()

        entry = fileDepot.getEntry(UPD_ID_1)
        assertEquals updateTime, entry.updated
        assertEquals 3, entry.findContents().size()
        assertEquals storedModified, entry.lastModified()
        assertEquals 2, entry.findEnclosures().size()
        assertTrue entry.getMetaFile("TEST_META_FILE").exists()
        // TODO:IMPROVE: verify path of enclosures..
    }


    /* TODO:? Really? Depot user must perhaps know the details..
    @Test
    void shouldOperateViaBatch() {
        def batch = depot.makeEntryBatch()
        batch.create
        assertTrue locked
        batch.update
        ...
        batch.delete
        ...
        batch.commit()
        assert hasBeenIndexed
    }
    */

}
