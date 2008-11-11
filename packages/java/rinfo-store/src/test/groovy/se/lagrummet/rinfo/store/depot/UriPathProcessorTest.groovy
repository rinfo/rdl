package se.lagrummet.rinfo.store.depot


class UriPathProcessorTest extends GroovyTestCase {

    def pathProcessor

    void setUp() {
        pathProcessor = new UriPathProcessor()
    }


    static final PATHS_TO_PARSE = [

        ["/serie/",
            new ParsedPath("serie", "/serie", true)],

        ["/serie/rdf",
            new ParsedPath("serie", "/serie", false, "rdf")],

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

        ["/serie/fs",
            new ParsedPath("serie", "/serie/fs")],

        ["/serie/fs.rdf",
            new ParsedPath("serie", "/serie/fs.rdf")],

        ["/publ/sfs/1999:175/image.png",
            new ParsedPath("publ", "/publ/sfs/1999:175/image.png")],

        ["/data/example.org-ap-12/A-34",
            new ParsedPath("data", "/data/example.org-ap-12/A-34")],

        ["/data/example.org-ap-12/A-34/rdf",
            new ParsedPath("data", "/data/example.org-ap-12/A-34", false, "rdf")],

        // .. edge case: "/coll/item/called/rdf" => "/coll/item/called/rdf/rdf" ..
    ]

    void testShouldMatchPaths() {
        PATHS_TO_PARSE.each {
            def path = it[0]
            def expected = it[1]
            def results = pathProcessor.parseUriPath(path)
            assertEquals expected, results
        }
    }


    static final PATHS_TO_MAKE = [
        [["/things/item/one", "application/xhtml+xml", null],
            "/things/item/one/xhtml"],

        [["/things/item/one", "application/xhtml+xml", "en"],
            "/things/item/one/xhtml,en"],

        [["/things/item/one", "UNKNOWN", "sv"],
                null],
    ]

    void testShouldMakeNegotiatedPaths() {
        PATHS_TO_MAKE.each {
            def params = it[0]
            def expected = it[1]
            def path = params[0], mtype = params[1], lang = params[2]
            if (expected) {
                def results = pathProcessor.makeNegotiatedUriPath(
                    path, mtype, lang)
                assertEquals expected, results
            } else {
                try {
                    pathProcessor.makeNegotiatedUriPath(path, mtype, lang)
                    fail()
                } catch (UnknownMediaTypeException e) {
                    // ok
                }
            }
        }
    }


    void testMediaTypeForHint() {
        assertEquals pathProcessor.mediaTypeForHint("pdf"), "application/pdf"
    }

    void testHintForMediaType() {
        assertEquals pathProcessor.hintForMediaType("application/pdf"), "pdf"
    }


}
