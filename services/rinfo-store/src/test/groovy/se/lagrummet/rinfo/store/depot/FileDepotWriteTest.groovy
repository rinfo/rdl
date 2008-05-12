package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.*

import org.apache.commons.io.FileUtils


class FileDepotWriteTest {

    static FileDepot fileDepot
    static File tempDepotDir

    @BeforeClass
    static void setupClass() {
        def manualTempDepotDir = System.properties["rinfo.manualTempDepotDir"]
        if (manualTempDepotDir) {
            tempDepotDir = new File(manualTempDepotDir)
        } else {
            def tempDir = new File(System.getProperty("java.io.tmpdir"))
            tempDepotDir = File.createTempFile("rinfodepot", "", tempDir)
            assert tempDepotDir.delete() // remove file to create dir..
            assert tempDepotDir.mkdir()
        }
        def depotSrc = new File("src/test/resources/exampledepot/storage")
        FileUtils.copyDirectoryToDirectory(depotSrc, tempDepotDir)
        fileDepot = new FileDepot(
                new URI("http://example.org"),
                new File(tempDepotDir, depotSrc.name))
    }

    @AfterClass
    static void tearDownClass() {
        // NOTE: to keep test depot dir, use:
        //  $ groovy -Drinfo.manualTempDepotDir=<depot-dir> <this-file>
        if (!System.properties["rinfo.manualTempDepotDir"]) {
            FileUtils.forceDelete(tempDepotDir)
        }
    }


    static final NEW_IDENTIFIER_1 = new URI("http://example.org/publ/NEW:1/added_1")

    @Test
    void shouldCreateEntry() {
        assertNull depot.getEntry(NEW_IDENTIFIER_1)

        def timestamp = new Date()
        fileDepot.createEntry(NEW_IDENTIFIER_1, timestamp,
                [
                    new DepotContent(exampleFile("content-en.pdf"),
                            "application/pdf", "en"),
                    new DepotContent(exampleFile("content.rdf"),
                            "application/rdf+xml")
                ],
                [ new DepotContent(exampleFile("icon.png"), null, "icon.png") ]
            )

        def entry = depot.getEntry(identifier)

        assertEquals entry.identifier, identifier
        assertEquals entry.published, entry.updated
        assertEquals entry.updated, timestamp
        assertEquals entry.deleted, false

        assert entry.findContents("application/pdf", "en")[0]
        assert entry.findContents("application/rdf+xml")[0]

        assertEquals entry.findEnclosures.size(), 1
        def encl = entry.findEnclosures()[0]
        assertEquals encl.public_path, "image.png"
        assertEquals encl.mtype, "image/png"
    }

    @Test
    void shouldUpdateEntry() {
    }

    @Test
    void shouldDeleteEntry() {
    }


    @Test
    void shouldGenerateAtomEntry() {
        def entry = fileDepot.getEntry("/publ/1901:100")
        assertEquals 0, entry.findContents(fileDepot.uriStrategy.
                hintForMediaType("application/atom+xml;type=entry")).size()
        entry.generateAtomEntryContent()
        //fileDepot.indexEntry(entry)
        def atomContent = entry.findContents(fileDepot.uriStrategy.
                hintForMediaType("application/atom+xml;type=entry"))[0]
        assert atomContent.file.isFile()
    }

    // TODO: shouldGenerateAtomEntryIfManifestModified
    // TODO: shouldGenerateAtomEntryWhenIndexingNewEntry ?

    @Test
    void shouldGenerateIndex() {
        fileDepot.generateIndex()
        // TODO: list feeds..
    }


}
