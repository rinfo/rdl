package se.lagrummet.rinfo.store.depot

import org.junit.runner.RunWith
import spock.lang.*


@Speck @RunWith(Sputnik)
class PathHandlerSpeck {

    @Shared pather = new PathHandler()

    def "should parse URIs"() {
        expect:
        pather.parseUriPath(path) == parsedPath

        where:
        [path, parsedPath] << [
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
        ]

    }

    def "should make URLs by content negotiation"() {
        expect:
        pather.makeNegotiatedUriPath(path, mtype, lang) == resultPath
        where:
        [path, mtype, lang, resultPath] << [
            ["/things/item/one", "application/xhtml+xml", null,
                    "/things/item/one/xhtml"],
            ["/things/item/one", "application/xhtml+xml", "en",
                    "/things/item/one/xhtml,en"]
        ]
    }

    def "should fail when making path from unknown media type"() {
        when:
        pather.makeNegotiatedUriPath("/things/item/one", "UNKNOWN", "sv")
        then:
        thrown(UnknownMediaTypeException)
    }

    def "should return media type for hint"() {
        pather.mediaTypeForHint("pdf") == "application/pdf"
    }

    def "should return hint for media type"() {
        pather.hintForMediaType("application/pdf") == "pdf"
    }

}
