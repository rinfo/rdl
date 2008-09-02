package se.lagrummet.rinfo.store.depot


class FileDepotReadTest extends GroovyTestCase {

    FileDepot fileDepot

    void setUp() {
        fileDepot = new FileDepot(
                new URI("http://example.org"),
                new File("src/test/resources/exampledepot/storage"),
                "/feed")
    }


    void testShouldContainEntry() {
        def entry = fileDepot.getEntry("/publ/1901/100")
        assertNotNull entry
        // TODO: see FileDepotWriteTest
        // assertEquals "", entry.id # updated, published
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

    void testShouldFindEnclosures() {
        // TODO: see FileDepotWriteTest
        // .. direct, several, nested
        // .. not list enclosures in nested entries
    }

    // TODO: getHistoricalEntries..

    void testShouldNotGetDeleted() {
        def deletedId = "/publ/1901/0"
        shouldFail(DeletedDepotEntryException) {
            def entry = fileDepot.getEntry(deletedId)
        }
    }

    void testShouldIterateEntries() {

        def entries = fileDepot.iterateEntries().toList()
        assertEquals 3, entries.size()

         // include deleted
         entries = fileDepot.iterateEntries(false, true)
        assertEquals 4, entries.size()

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

}
