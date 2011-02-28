package se.lagrummet.rinfo.base

import org.openrdf.rio.RDFFormat

import se.lagrummet.rinfo.base.rdf.RDFUtil

import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.xml.dom.DOMUtil

import spock.lang.*


class URIMinterSpec extends Specification {

    @Shared URIMinter uriMinter
    @Shared String testFeedPath

    def setupSpec() {
        def baseDir = "../../../resources/base"
        def repo = RDFUtil.slurpRdf("${baseDir}/sys/uri")
        def schemeUri = "http://rinfo.lagrummet.se/sys/uri/scheme#"
        uriMinter = new URIMinter(repo, schemeUri)
        def minterDir = "${baseDir}/uri_algorithm"
        testFeedPath = "${minterDir}/tests/publ.atom"
    }

    def "should mint uri from stream"() {
        when:
        def feed = DOMBuilder.parse(new FileReader(testFeedPath)).documentElement
        then:
        use (DOMCategory) {
            feed.entry.each {
                def expectedUri = it.id.text()
                def content = it.content[0]
                def data = DOMUtil.serialize(content.'*'[0])
                def format = RDFFormat.forMIMEType(content.'@type')
                def repo = RDFUtil.createMemoryRepository()
                def conn = repo.getConnection()
                try {
                    conn.add(new StringReader(data), "", format)
                    def computedUri = uriMinter.computeUri(repo)
                    //"Error in entry:\n${it}\n"
                    assert expectedUri == computedUri
                } finally {
                    conn.close()
                }
            }
        }
    }

}
