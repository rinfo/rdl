package se.lagrummet.rinfo.base

import org.junit.Test
import org.junit.BeforeClass
import static org.junit.Assert.*

import se.lagrummet.rinfo.base.rdf.RDFUtil


class URIMinterTest {

    static URIMinter uriMinter

    @BeforeClass
    static void setupClass() {
        def rinfoBaseDir = "../../../resources/base/"
        uriMinter = new URIMinter(rinfoBaseDir)
    }

    @Test
    void shouldMintURIFromStream() {
        def repo = RDFUtil.createMemoryRepository()
        /* FIXME: test data and implement
        def sourceStream = null
        def mediaType = null
        RDFUtil.loadDataFromStream(repo, sourceStream, "", mediaType)
        assertEquals "", uriMinter.computeOfficialUri(repo, RDFFormat.RDFXML)
        */
    }

}
