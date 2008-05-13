package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.*


class FileDepotWriteTest {

    static FileDepot fileDepot
    static File tempDepotDir
    static File depotSrc

    @BeforeClass
    static void setupClass() {
        depotSrc = new File("src/test/resources/exampledepot/storage")
        tempDepotDir = TempDirUtil.createTempDir(depotSrc)
        fileDepot = new FileDepot(new URI("http://example.org"),
                new File(tempDepotDir, depotSrc.name))
    }

    @AfterClass
    static void tearDownClass() {
        TempDirUtil.removeTempDir(tempDepotDir)
    }


    static final NEW_IDENTIFIER_1 = new URI("http://example.org/publ/NEW:1/added_1")
    static final DEL_IDENTIFIER_1 = new URI("http://example.org/publ/DEL:0/deleted_1")

    @Test
    void shouldCreateEntry() {
        assertNull fileDepot.getEntry(NEW_IDENTIFIER_1)

        def timestamp = new Date()
        fileDepot.createEntry(NEW_IDENTIFIER_1, timestamp,
                [
                    new DepotContent(exampleContentFile("content-en.pdf"),
                            null, "application/pdf", "en"),
                    new DepotContent(exampleContentFile("content.rdf"),
                            null, "application/rdf+xml")
                ],
                // TODO: with enclosures..
                //[ new DepotContent(exampleFile("icon.png"), "icon.png", null) ]
            )

        def entry = fileDepot.getEntry(NEW_IDENTIFIER_1)

        assertEquals entry.id, NEW_IDENTIFIER_1
        assertEquals entry.published, entry.updated
        assertEquals entry.updated, timestamp
        assertEquals entry.deleted, false

        assert entry.findContents("application/pdf", "en")[0]
        assert entry.findContents("application/rdf+xml")[0]

        // TODO: -||-
        //assertEquals entry.findEnclosures.size(), 1
        //def encl = entry.findEnclosures()[0]
        //assertEquals encl.public_path, "image.png"
        //assertEquals encl.mtype, "image/png"
    }

    @Test
    void shouldUpdateEntry() {
        def entry = fileDepot.getEntry(NEW_IDENTIFIER_1)
        def timestamp = new Date()
        entry.update(timestamp,
                [ new DepotContent(exampleContentFile("content-en.pdf"),
                            null, "application/pdf", "en") ]
            )
        // TODO: assert..
    }

    @Test
    void shouldDeleteEntry() {
        // TODO
        assertNull fileDepot.getEntry(DEL_IDENTIFIER_1)
        def created = new Date()
        fileDepot.createEntry(DEL_IDENTIFIER_1, created, [])

        def deleted = new Date()
        def entry = fileDepot.getEntry(DEL_IDENTIFIER_1)
        entry.delete(deleted)

        entry = fileDepot.getEntry(DEL_IDENTIFIER_1)
        assertEquals entry.edited, deleted
        assertEquals entry.deleted, true
    }


    protected exampleContentFile(path) {
        new File(depotSrc, "publ/1901/100/ENTRY-INFO/${path}")
    }


}
