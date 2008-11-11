package se.lagrummet.rinfo.store.depot


class FileDepotReadTest extends GroovyTestCase {

    FileDepot fileDepot

    void setUp() {
        fileDepot = FileDepot.newConfigured(
                "src/test/resources/rinfo-depot.properties")
    }


    void testShouldContainEntry() {
        def entry = fileDepot.getEntry("/publ/1901/100")
        assertNotNull entry
        assertEquals entry.id, new URI("http://example.org/publ/1901/100")
        assertEquals entry.published, entry.updated
    }

    void testShouldFindEntryContent() {
        def entry = fileDepot.getEntry("/publ/1901/100")
        def contents = entry.findContents("application/pdf")
        def i = 0
        contents.each {
            assertEquals it.mediaType, "application/pdf"
            if (it.lang == "en") {
                i++
                assertEquals "/publ/1901/100/pdf,en", it.depotUriPath
            } else if (it.lang == "sv") {
                i++
                assertEquals "/publ/1901/100/pdf,sv", it.depotUriPath
            }
        }
        assertEquals 2, i
        contents = entry.findContents("application/rdf+xml")
        def c = contents[0]
        assertEquals 1, contents.size()
        assertEquals "application/rdf+xml", c.mediaType
        assertEquals "/publ/1901/100/rdf", c.depotUriPath
        assertNull c.lang
    }

    void testShouldFindEnclosure() {
        def enclosures = fileDepot.getEntry("/publ/1901/100").findEnclosures()
        assertEquals 1, enclosures.size()
        def encl = enclosures[0]
        assertEquals "/publ/1901/100/icon.png", encl.depotUriPath
        assertEquals "image/png", encl.mediaType
    }

    void testShouldFindNestedEnclosure() {
        def entryPath = "/publ/1901/100/revisions/1902/200"
        def enclosures = fileDepot.getEntry(entryPath).findEnclosures()
        def encl = enclosures[0]
        assertEquals "${entryPath}/styles/screen.css", encl.depotUriPath
        // TODO: needs to configure FileDepot computeMediaType
        //assertEquals "text/css", encl.mediaType
    }

    void testShouldNotGetDeleted() {
        def deletedId = "/publ/1901/0"
        shouldFail(DeletedDepotEntryException) {
            def entry = fileDepot.getEntry(deletedId)
        }
    }

    void testShouldFindAllEntryContentAsList() {
        def results = fileDepot.find("/publ/1901/100")
        assertEquals 3, results.size()
    }

    void testShouldFindDirectContent() {
        def results = fileDepot.find("/publ/1901/100/icon.png")
        assertEquals 1, results.size()
    }

    // TODO: getHistoricalEntries..

    void testShouldIterateEntries() {

        def entries = fileDepot.iterateEntries().toList()
        assertEquals 5, entries.size()

         // include deleted
         entries = fileDepot.iterateEntries(false, true)
        assertEquals 6, entries.size()

        // TODO: historical: fileDepot.iterateEntries(true, false)
    }


    // negative tests

    void testShouldDisallowUrisNotWithinBaseUri() {
        shouldFail(DepotUriException) {
            def entry = fileDepot.getEntry(
                    new URI("http://example.com/some/path"))
        }
    }

    void testShouldDisallowNonAbsoluteOrFullUriPaths() {
        shouldFail(DepotUriException) {
            def entry = fileDepot.getEntry("http://example.com/some/path")
        }
        shouldFail(DepotUriException) {
            def entry = fileDepot.getEntry("http://example.org/some/path")
        }
    }

    // edge cases / regressions

    void testShouldFindRdfInTopEntry() {
        def entry = fileDepot.getEntry("/dataset")
        assertNotNull entry
        def contents = entry.findContents("application/rdf+xml", null)
        assertEquals 1, contents.size()
        def c = contents[0]
        assertEquals "application/rdf+xml", c.mediaType
        assertEquals "/dataset/rdf", c.depotUriPath
    }

}
