package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.*


class FileDepotWriteTest extends FileDepotTempBase {

    static final NEW_ID_1 = new URI("http://example.org/publ/NEW/added_1")
    static final NEW_ID_2 = new URI("http://example.org/publ/NEW/added_2")
    static final NEW_ID_3 = new URI("http://example.org/publ/NEW/added_3")
    static final UPD_ID_1 = new URI("http://example.org/publ/UPD/updated_1")
    static final UPD_ID_2 = new URI("http://example.org/publ/UPD/updated_2")
    static final UPD_ID_3 = new URI("http://example.org/publ/UPD/updated_3")
    static final DEL_ID_1 = new URI("http://example.org/publ/DEL/deleted_1")
    static final CHECKED_ID_1 = new URI("http://example.org/publ/CHECK/added_1")
    static final FAILED_ID_1 = new URI("http://example.org/publ/CHECK/failed_1")
    static final FAILED_ID_2 = new URI("http://example.org/publ/CHECK/failed_2")


    @BeforeClass static void setupClass() { createTempDepot() }
    @AfterClass static void tearDownClass() { deleteTempDepot() }


    @Test
    void shouldCreateEntry() {
        assertNull depot.getEntry(NEW_ID_1)

        def createTime = new Date()
        depot.createEntry(NEW_ID_1, createTime,
                [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
                    new SourceContent(exampleEntryFile("content.rdf"),
                            "application/rdf+xml")
                ],
            )

        def entry = depot.getEntry(NEW_ID_1)
        assertFalse entry.isLocked()

        assertEquals entry.id, NEW_ID_1
        assertEquals entry.published, entry.updated
        assertEquals entry.updated, createTime
        assertEquals entry.deleted, false

        assertNotNull entry.findContents("application/pdf", "en")[0]
        assertNotNull entry.findContents("application/rdf+xml")[0]
    }


    @Test
    void shouldCreateEntryWithEnclosures() {
        assertNull depot.getEntry(NEW_ID_2)
        depot.createEntry(NEW_ID_2, new Date(),
                [],
                [
                    // full path
                    new SourceContent(exampleFile("icon.png"),
                            null, null,
                            "/publ/NEW/added_2/icon.png"),

                    // relative to entry path
                    new SourceContent(exampleFile("icon.png"),
                            null, null,
                            "icon2.png"),

                    // nested path
                    new SourceContent(exampleFile("icon.png"),
                            null, null,
                            "images/icon.png"),
                ]
            )
        def entry = depot.getEntry(NEW_ID_2)
        def enclosures = entry.findEnclosures()
        assertEquals 3, enclosures.size()

        def expect = { path ->
            def encl = enclosures.find { it.depotUriPath == path }
            assertNotNull encl
            assertEquals "image/png", encl.mediaType
        }

        expect "/publ/NEW/added_2/icon.png"
        expect "/publ/NEW/added_2/icon2.png"
        expect "/publ/NEW/added_2/images/icon.png"
    }

    @Test
    void shouldCreateLockedEntry() {
        assertNull depot.getEntry(NEW_ID_3)
        def createTime = new Date()
        def entry = depot.createEntry(NEW_ID_3, createTime,
                [ new SourceContent(exampleEntryFile("content.rdf"),
                            "application/rdf+xml") ],
                false
            )
        assertTrue entry.isLocked()
        entry.unlock()
        assertFalse entry.isLocked()
        entry = depot.getEntry(NEW_ID_3)
        assertNotNull entry
        assertFalse entry.isLocked()
    }


    @Test(expected=DepotUriException)
    void shouldFailOnEnclosureOutOfPath() {
        def BAD_ENCL_1 = new URI("http://example.org/publ/ERROR/encl_1")
        def invalidEnclPath = "/publ/OTHER/path/icon.png"
        depot.createEntry(BAD_ENCL_1, new Date(),
                [],
                [ new SourceContent(exampleFile("icon.png"),
                            null, null, invalidEnclPath), ]
            )
    }


    @Test
    void shouldUpdateEntry() {
        assertNull depot.getEntry(UPD_ID_1)

        def createTime = new Date()
        depot.createEntry(UPD_ID_1, createTime, [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])
        def entry = depot.getEntry(UPD_ID_1)
        assertNotNull entry
        assertEquals 1, entry.findContents("application/pdf").size()
        assertEquals entry.published, entry.updated
        assertEquals entry.updated, createTime

	Thread.sleep(100)
        def updateTime = new Date()
        entry.update(updateTime, [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
                new SourceContent(exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv")
            ])
        assertEquals 2, entry.findContents("application/pdf").size()
        assertTrue entry.updated > entry.published
        assertEquals entry.published, createTime
        assertEquals entry.updated, updateTime
        assertEquals entry.deleted, false
        // TODO: getHistoricalEntries..
        assertFalse entry.isLocked()
    }


    @Test
    void shouldUpdateEntryWithLessContents() {
        depot.createEntry(UPD_ID_2, new Date(), [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
                new SourceContent(exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv")
            ])
        def entry = depot.getEntry(UPD_ID_2)
        entry.update(new Date(), [
                new SourceContent(exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv"),
            ])
        assertEquals "sv", entry.contentLanguage
        def contents = entry.findContents("application/pdf")
        assertEquals 1, contents.size()
        assertEquals "sv", contents[0].lang

    }

    @Test
    void shouldMoveEnclosuresWhenUpdatingEntry() {
        def entry = depot.createEntry(UPD_ID_3, new Date(),
                [new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")],
                [
                    new SourceContent(exampleFile("icon.png"),
                            null, null,
                            "icon2.png"),
                    new SourceContent(exampleFile("icon.png"),
                            null, null,
                            "images/icon.png"),
                ]
            )
        entry.update(new Date(), [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
            ])
        def enclosures = entry.findEnclosures()
        assertEquals 0, enclosures.size()
        // TODO: verify *not* moving encls in nested entries!
    }

    @Test
    void shouldDeleteEntry() {
        assertNull depot.getEntry(DEL_ID_1)
        def createTime = new Date()
        depot.createEntry(DEL_ID_1, createTime, [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])

        def deleteTime = new Date()
        def entry = depot.getEntry(DEL_ID_1)
        entry.delete(deleteTime)

        entry = depot.getUncheckedDepotEntry(DEL_ID_1.path)
        assertFalse entry.isLocked()
        assertEquals 0, entry.findContents("application/pdf").size()
        assertEquals entry.updated, deleteTime
        assertEquals entry.deleted, true
    }

    @Test
    void shouldCreateEntryAndCheckMD5AndLength() {
        def srcContent = new SourceContent(
                exampleEntryFile("content-en.pdf"), "application/pdf", "en")
        srcContent.datachecks[SourceContent.Check.LENGTH] = new Long(24014)
        srcContent.datachecks[SourceContent.Check.MD5] =
                "eff60b86aaaac3a1fde5affc07a27006"
        depot.createEntry(CHECKED_ID_1, new Date(), [srcContent])
    }


    @Test(expected=SourceCheckException)
    void shouldFailCreateEntryOnBadMD5() {
        def srcContent = new SourceContent(
                exampleEntryFile("content-en.pdf"), "application/pdf", "en")
        srcContent.datachecks[SourceContent.Check.MD5] = "BAD_CHECKSUM"
        depot.createEntry(FAILED_ID_1, new Date(), [srcContent])
    }


    @Test(expected=SourceCheckException)
    void shouldFailCreateEntryOnBadLength() {
        def srcContent = new SourceContent(
                exampleEntryFile("content-en.pdf"), "application/pdf", "en")
        srcContent.datachecks[SourceContent.Check.LENGTH] = new Long(0)
        depot.createEntry(FAILED_ID_2, new Date(), [srcContent])
    }


}
