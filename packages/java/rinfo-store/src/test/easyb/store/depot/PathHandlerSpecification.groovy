import se.lagrummet.rinfo.store.depot.PathHandler
import se.lagrummet.rinfo.store.depot.ParsedPath
import se.lagrummet.rinfo.store.depot.UnknownMediaTypeException

pather = new PathHandler()

it "should parse URIs", {

    def parsed = pather.&parseUriPath

    parsed("/serie/").
        shouldBe new ParsedPath("serie", "/serie", true)

    parsed("/serie/").
        shouldBe new ParsedPath("serie", "/serie", true)

    parsed("/serie/rdf").
        shouldBe new ParsedPath("serie", "/serie", false, "rdf")

    parsed("/publ/sfs/1999:175").
        shouldBe new ParsedPath("publ", "/publ/sfs/1999:175")

    parsed("/publ/sfs/1999:175/").
        shouldBe new ParsedPath("publ", "/publ/sfs/1999:175", true)

    parsed("/publ/sfs/1999:175/rdf").
        shouldBe new ParsedPath("publ", "/publ/sfs/1999:175", false, "rdf")

    parsed("/publ/sfs/1999:175/pdf").
        shouldBe new ParsedPath("publ", "/publ/sfs/1999:175", false, "pdf")

    parsed("/publ/sfs/1999:175/xhtml,sv").
        shouldBe new ParsedPath("publ", "/publ/sfs/1999:175", false, "xhtml", "sv")

    parsed("/publ/dv/nja/2002/03:18").
        shouldBe new ParsedPath("publ", "/publ/dv/nja/2002/03:18", false)

    parsed("/ns/2007/09/rinfo/publ").
        shouldBe new ParsedPath("ns", "/ns/2007/09/rinfo/publ")

    parsed("/ns/2007/09/rinfo/publ,sv").
        shouldBe new ParsedPath("ns", "/ns/2007/09/rinfo/publ", false, null, "sv")

    parsed("/serie/fs").
        shouldBe new ParsedPath("serie", "/serie/fs")

    parsed("/serie/fs.rdf").
        shouldBe new ParsedPath("serie", "/serie/fs.rdf")

    parsed("/publ/sfs/1999:175/image.png").
        shouldBe new ParsedPath("publ", "/publ/sfs/1999:175/image.png")

    parsed("/data/example.org-ap-12/A-34").
        shouldBe new ParsedPath("data", "/data/example.org-ap-12/A-34")

    parsed("/data/example.org-ap-12/A-34/rdf").
        shouldBe new ParsedPath("data", "/data/example.org-ap-12/A-34", false, "rdf")

}

it "should make URLs by content negotiation", {

    def pathFrom = pather.&makeNegotiatedUriPath

    pathFrom("/things/item/one", "application/xhtml+xml").
        shouldBe "/things/item/one/xhtml"

    pathFrom("/things/item/one", "application/xhtml+xml", "en").
        shouldBe "/things/item/one/xhtml,en"

    ensureThrows(UnknownMediaTypeException) {
        pathFrom("/things/item/one", "UNKNOWN", "sv")
    }

}

it "should return media type for hint", {
    pather.mediaTypeForHint("pdf").shouldBe "application/pdf"
}

it "should return hint for media type", {
    pather.hintForMediaType("application/pdf").shouldBe "pdf"
}

