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
        def baseDir = "../../../resources/base"
        def repo = RDFUtil.slurpRdf("${baseDir}/sys/uri")
        uriMinter = new URIMinter(repo)
        def minterDir = "${baseDir}/uri_algorithm"
        testFeedPath = "${minterDir}/tests/publ.atom"
    }

    @Test
    void shouldMintURIFromStream() {
        def sourceStream = null

        def feed = DOMBuilder.parse(new FileReader(testFeedPath)).documentElement
        use (DOMCategory) {
            feed.entry.each {
                def expectedUri = new URI(it.id.text())
                def content = it.content[0]
                def data = DOMUtil.serialize(content.'*'[0])
                def format = RDFFormat.forMIMEType(content.'@type')
                def repo = RDFUtil.createMemoryRepository()
                def conn = repo.getConnection()
                try {
                    conn.add(new StringReader(data), "", format)
                    def computedUri = uriMinter.computeUri(repo)
                    assertEquals "Error in entry:\n${it}\n",
                            expectedUri, computedUri
                } finally {
                    conn.close()
                }
            }
        }
    }

}
