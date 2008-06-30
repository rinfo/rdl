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
                    new SourceContent(exampleContentFile("content-en.pdf"),
                            "application/pdf", "en"),
                    new SourceContent(exampleContentFile("content.rdf"),
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


    /* TODO
    @Test
    void shouldCreateEntryWithEnclosures() {
        // TODO: with enclosures..
        // NEW_ID_2
        //[ new SourceContent(exampleFile("icon.png"), "icon.png", null) ]

        // TODO:
        //assertEquals entry.findEnclosures.size(), 1
        //def encl = entry.findEnclosures()[0]
        //assertEquals encl.public_path, "image.png"
        //assertEquals encl.mtype, "image/png"
    }
    */


    @Test
    void shouldUpdateEntry() {
        assertNull fileDepot.getEntry(UPD_ID_1)

        def createTime = new Date()
        fileDepot.createEntry(UPD_ID_1, createTime, [
                new SourceContent(exampleContentFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])
        def entry = fileDepot.getEntry(UPD_ID_1)
        assertNotNull entry
        assertEquals 1, entry.findContents("application/pdf").size()
        assertEquals entry.published, entry.updated
        assertEquals entry.updated, createTime

        def updateTime = new Date()
        entry.update(updateTime, [
                new SourceContent(exampleContentFile("content-en.pdf"),
                            "application/pdf", "en"),
                new SourceContent(exampleContentFile("content-sv.pdf"),
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
                new SourceContent(exampleContentFile("content-en.pdf"),
                        "application/pdf", "en"),
                new SourceContent(exampleContentFile("content-sv.pdf"),
                            "application/pdf", "sv")
            ])
        def entry = fileDepot.getEntry(UPD_ID_2)
        entry.update(new Date(), [
                new SourceContent(exampleContentFile("content-sv.pdf"),
                            "application/pdf", "sv"),
            ])
        assertEquals "sv", entry.contentLanguage
        def contents = entry.findContents("application/pdf")
        assertEquals 1, contents.size()
        assertEquals "sv", contents[0].lang
    }


    @Test
    void shouldDeleteEntry() {
        assertNull fileDepot.getEntry(DEL_ID_1)
        def createTime = new Date()
        fileDepot.createEntry(DEL_ID_1, createTime, [
                new SourceContent(exampleContentFile("content-en.pdf"),
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


    protected exampleContentFile(path) {
        new File(depotSrc, "publ/1901/100/ENTRY-INFO/${path}")
    }


}
