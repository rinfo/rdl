package se.lagrummet.rinfo.base

import org.junit.Test
import org.junit.BeforeClass
import static org.junit.Assert.*

import org.openrdf.rio.RDFFormat

import se.lagrummet.rinfo.base.rdf.RDFUtil

import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.xml.dom.DOMUtil


class URIMinterTest {

    static URIMinter uriMinter
    static String testFeedPath

    @BeforeClass
    static void setupClass() {
        def rinfoBaseDir = "../../../resources/base/"
        uriMinter = new URIMinter(rinfoBaseDir)
        testFeedPath = rinfoBaseDir + "uri_algorithm/tests/publ.atom"
    }

    @Test
    void shouldMintURIFromStream() {
        def repo = RDFUtil.createMemoryRepository()
        def sourceStream = null

        def feed = DOMBuilder.parse(new FileReader(testFeedPath)).documentElement
        use (DOMCategory) {
            feed.entry.each {
                def expectedUri = new URI(it.id.text())
                def content = it.content[0]
                def data = DOMUtil.serialize(content.'*'[0])
                def format = RDFFormat.forMIMEType(content.'@type')
                def conn = repo.connection
                try {
                    conn.add(new StringReader(data), "", format)
                    assertEquals expectedUri,
                            uriMinter.computeOfficialUri(repo)
                } finally {
                    conn.close()
                }
            }
        }
    }

}
