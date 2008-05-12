package se.lagrummet.rinfo.store.depot


class FileDepotReadTest extends GroovyTestCase {

    FileDepot fileDepot

    void setUp() {
        fileDepot = new FileDepot(
                new URI("http://example.org"),
                new File("src/test/resources/exampledepot/storage"))
    }


    void testShouldContainEntry() {
        def entry = fileDepot.getEntry("/publ/1901/100")
        assertNotNull entry
    }


}
