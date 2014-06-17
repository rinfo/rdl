package se.lagrummet.rinfo.service

import spock.lang.*

import org.apache.abdera.Abdera

import org.openrdf.model.vocabulary.RDF

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.ParserConfig


class RepoEntrySpec extends Specification {

    static resourceUri = "http://rinfo.lagrummet.se/publ/mfs/1234:56"
    static publisherUri = "http://rinfo.lagrummet.se/org/myndighet_1"
    static updated = "2008-10-10T20:00:00.000Z"

    static entry = Abdera.instance.parser.parse(
            new ByteArrayInputStream("""
        <entry xmlns="http://www.w3.org/2005/Atom">
            <id>${resourceUri}</id>
            <updated>${updated}</updated>
            <title type="text"></title>
            <content type="application/rdf+xml" src="${resourceUri}.rdf"/>
        </entry>""".getBytes("utf-8"))).root

    static rdfBytes = """
        <rpubl:Myndighetsforeskrift rdf:about="${resourceUri}"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
                xmlns:dct="http://purl.org/dc/terms/">
            <dct:title xml:lang="en">Thing 1</dct:title>
            <dct:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date"
                        >2008-10-08</dct:created>
            <dct:publisher rdf:resource="${publisherUri}"/>
            <dct:relation rdf:resource="http://example.org/other"/>
        </rpubl:Myndighetsforeskrift>"""

    static rdfBytes_with_a_href_element = """
        <rpubl:Myndighetsforeskrift rdf:about="${resourceUri}"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
                xmlns:dct="http://purl.org/dc/terms/">
            <dct:title xml:lang="en">Thing 1</dct:title>
            <dct:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date"
                        >2008-10-08</dct:created>
            <dct:publisher rdf:resource="${publisherUri}"/>
            <dct:relation rdf:resource="http://example.org/other"/>
            <a href="http://somedomain.se/000655.pdf">PDF</a>
        </rpubl:Myndighetsforeskrift>"""

    RepoEntry repoEntry
    Describer desc
    SesameLoader loader
    RepositoryConnection conn

    def setup() {
        def repo = RDFUtil.createMemoryRepository()
        conn = repo.connection
        loader = Mock()
    }

    def "should add entry for resource"() {
        given:
        loader.getConn() >> conn
        loader.getResponseAsInputStream(_) >> {
            new ByteArrayInputStream(rdfBytes.getBytes("utf-8"))
        }

        when:
        repoEntry = new RepoEntry(loader, entry)
        repoEntry.create()
        desc = new Describer(conn, false, repoEntry.context.stringValue()).
            setPrefix("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#").
            setPrefix("foaf", "http://xmlns.com/foaf/0.1/")
        def atomEntry = desc.subjects("foaf:primaryTopic", resourceUri).toList()[0]

        then:
        atomEntry.type.about == desc.expandCurie("awol:Entry")
        atomEntry.getString("awol:updated") == updated
        atomEntry.getRel("awol:content").about == "${resourceUri}.rdf"
        atomEntry.getRel("awol:content").getString("awol:type") == "application/rdf+xml"
    }

    def "should catch RDFParseException if parse error"() {
        given:
        setStopAtFirstErrorToTriggerRDFParseException()
        loader.getConn() >> conn
        loader.getResponseAsInputStream(_) >> {
            new ByteArrayInputStream(rdfBytes_with_a_href_element.getBytes("utf-8"))
        }

        when:
        repoEntry = new RepoEntry(loader, entry)
        repoEntry.create()

        then:
        notThrown(RDFParseException) // since caught by create()
    }

    def setStopAtFirstErrorToTriggerRDFParseException() {
        boolean stopAtFirstError = true
        ParserConfig newConfig = new ParserConfig(conn.getParserConfig().verifyData(),
                                                  stopAtFirstError,
                                                  conn.getParserConfig().isPreserveBNodeIDs(),
                                                  conn.getParserConfig().datatypeHandling());
        conn.setParserConfig(newConfig);
    }

}
