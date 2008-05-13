package se.lagrummet.rinfo.store.depot


class FileDepotAtomIndexTest {

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
