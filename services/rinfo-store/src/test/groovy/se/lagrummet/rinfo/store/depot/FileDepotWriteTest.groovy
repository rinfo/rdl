package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.*


class FileDepotWriteTest {

    static FileDepot fileDepot
    static File tempDepotDir
    static File depotSrc

    static final NEW_ID_1 = new URI("http://example.org/publ/NEW/added_1")
    static final NEW_ID_2 = new URI("http://example.org/publ/NEW/added_2")
    static final UPD_ID_1 = new URI("http://example.org/publ/UPD/updated_1")
    static final UPD_ID_2 = new URI("http://example.org/publ/UPD/updated_2")
    static final DEL_ID_1 = new URI("http://example.org/publ/DEL/deleted_1")


    @BeforeClass
    static void setupClass() {
        depotSrc = new File("src/test/resources/exampledepot/storage")
        tempDepotDir = TempDirUtil.createTempDir(depotSrc)
        fileDepot = new FileDepot(new URI("http://example.org"),
                new File(tempDepotDir, depotSrc.name), "feed")
    }

    @AfterClass
    static void tearDownClass() {
        TempDirUtil.removeTempDir(tempDepotDir)
    }


    @Test
    void shouldCreateEntry() {
        assertNull fileDepot.getEntry(NEW_ID_1)

        def createTime = new Date()
        fileDepot.createEntry(NEW_ID_1, createTime,
                [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
                    new SourceContent(exampleEntryFile("content.rdf"),
                            "application/rdf+xml")
                ],
            )

        def entry = fileDepot.getEntry(NEW_ID_1)

        assertEquals entry.id, NEW_ID_1
        assertEquals entry.published, entry.updated
        assertEquals entry.updated, createTime
        assertEquals entry.deleted, false

        assertNotNull entry.findContents("application/pdf", "en")[0]
        assertNotNull entry.findContents("application/rdf+xml")[0]
    }


    @Test
    void shouldCreateEntryWithEnclosures() {
        assertNull fileDepot.getEntry(NEW_ID_2)
        fileDepot.createEntry(NEW_ID_2, new Date(),
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

                    // TODO: invalid path (outside of entryUriPath)
                ]
            )
        def entry = fileDepot.getEntry(NEW_ID_2)
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
    void shouldUpdateEntry() {
        assertNull fileDepot.getEntry(UPD_ID_1)

        def createTime = new Date()
        fileDepot.createEntry(UPD_ID_1, createTime, [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])
        def entry = fileDepot.getEntry(UPD_ID_1)
        assertNotNull entry
        assertEquals 1, entry.findContents("application/pdf").size()
        assertEquals entry.published, entry.updated
        assertEquals entry.updated, createTime

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
    }


    @Test
    void shouldUpdateEntryWithLessContents() {
        fileDepot.createEntry(UPD_ID_2, new Date(), [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
                new SourceContent(exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv")
            ])
        def entry = fileDepot.getEntry(UPD_ID_2)
        entry.update(new Date(), [
                new SourceContent(exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv"),
            ])
        assertEquals "sv", entry.contentLanguage
        def contents = entry.findContents("application/pdf")
        assertEquals 1, contents.size()
        assertEquals "sv", contents[0].lang

        // TODO: update must move encloures, including nested encloures,
        //but *not* nested entries!
    }


    @Test
    void shouldDeleteEntry() {
        assertNull fileDepot.getEntry(DEL_ID_1)
        def createTime = new Date()
        fileDepot.createEntry(DEL_ID_1, createTime, [
                new SourceContent(exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])

        def deleteTime = new Date()
        def entry = fileDepot.getEntry(DEL_ID_1)
        entry.delete(deleteTime)

        entry = fileDepot.getEntry(DEL_ID_1, false)
        assertEquals 0, entry.findContents("application/pdf").size()
        assertEquals entry.updated, deleteTime
        assertEquals entry.deleted, true
    }


    protected exampleEntryFile(path) {
        new File(depotSrc, "publ/1901/100/ENTRY-INFO/${path}")
    }

    protected exampleFile(path) {
        new File(depotSrc, "publ/1901/100/${path}")
    }


}
