package se.lagrummet.rinfo.store.depot


class DepotUriStrategyTest extends GroovyTestCase {

    def uriStrategy

    void setUp() {
        uriStrategy = new DepotUriStrategy()
    }


    static final PATHS_TO_PARSE = [

        ["/publ/sfs/1999:175",
            new ParsedPath("publ", "/publ/sfs/1999:175")],

        ["/publ/sfs/1999:175/", 
            new ParsedPath("publ", "/publ/sfs/1999:175", true)],

        ["/publ/sfs/1999:175/rdf",
            new ParsedPath("publ", "/publ/sfs/1999:175", false, "rdf")],

        ["/publ/sfs/1999:175/pdf",
            new ParsedPath("publ", "/publ/sfs/1999:175", false, "pdf")],

        ["/publ/sfs/1999:175/xhtml,sv",
            new ParsedPath("publ", "/publ/sfs/1999:175", false, "xhtml", "sv")],

        ["/publ/dv/nja/2002/03:18",
            new ParsedPath("publ", "/publ/dv/nja/2002/03:18", false)],

        ["/ns/2007/09/rinfo/publ",
            new ParsedPath("ns", "/ns/2007/09/rinfo/publ")],

        ["/ns/2007/09/rinfo/publ,sv",
            new ParsedPath("ns", "/ns/2007/09/rinfo/publ", false, null, "sv")],

        ["/publ/sfs/1999:175/image.png",
            new ParsedPath("publ", "/publ/sfs/1999:175/image.png")],

        // .. edge case: "/coll/item/called/rdf" => "/coll/item/called/rdf/rdf" ..
    ]

    void testShouldMatchPaths() {
        PATHS_TO_PARSE.each {
            def path = it[0]
            def expected = it[1]
            def results = uriStrategy.parseUriPath(path)
            assertEquals expected, results
        }
    }


    static final PATHS_TO_MAKE = [
        [["/things/item/one", "xhtml", null], "/things/item/one/xhtml"],
        [["/things/item/one", "xhtml", "en"], "/things/item/one/xhtml,en"],
        [["/things/item/one", "UNKNOWN", "sv"], null],
    ]

    void testShouldMakeNegotiatedPaths() {
        PATHS_TO_MAKE.each {
            def params = it[0]
            def expected = it[1]
            def path = params[0], hint = params[1], lang = params[2]
            if (expected) {
                def results = uriStrategy.makeNegotiatedUriPath(
                    path, hint, lang)
                assertEquals expected, results
            } else {
                try {
                    uriStrategy.makeNegotiatedUriPath(path, hint, lang)
                    fail()
                } catch (AssertionError e) {
                    // ok
                }
            }
        }
    }


    void testMediaTypeForHint() {
        assertEquals uriStrategy.mediaTypeForHint("pdf"), "application/pdf"
    }

    void testHintForMediaType() {
        assertEquals uriStrategy.hintForMediaType("application/pdf"), "pdf"
    }


}
