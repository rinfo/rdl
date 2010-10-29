package se.lagrummet.rinfo.service

import spock.lang.*

import org.apache.abdera.Abdera

import org.openrdf.model.vocabulary.RDF

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer


class RepoEntrySpec extends Specification {

    static resourceUri = "http://example.org/thing/1"
    static publisherUri = "http://example.org/publisher/1"
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
        <owl:Thing rdf:about="${resourceUri}"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:dct="http://purl.org/dc/terms/">
            <dct:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date"
                        >2008-10-08</dct:created>
            <dct:publisher rdf:resource="${publisherUri}"/>
        </owl:Thing>"""


    Describer desc

    def setup() {
        def repo = RDFUtil.createMemoryRepository()
        def conn = repo.connection
        SesameLoader loader = Mock()
        loader.getConn() >> conn
        loader.getResponseAsInputStream(_) >> {
            new ByteArrayInputStream(rdfBytes.getBytes("utf-8"))
        }
        RepoEntry repoEntry = new RepoEntry(loader, entry)
        repoEntry.create()
        desc = newDescriber(repoEntry.conn)
    }

    def "should add entry for resource"() {
        given:
        def atomEntry = desc.subjects("foaf:primaryTopic", resourceUri)[0]
        expect:
        atomEntry.type.about == desc.expandCurie("awol:Entry")
        atomEntry.getValue("awol:updated") == updated
        atomEntry.getRel("awol:content").about == "${resourceUri}.rdf"
        atomEntry.getRel("awol:content").getValue("awol:type") == "application/rdf+xml"
    }

    def "should add statistics for resource"() {
        given:
        def statItem = desc.getByType("scv:Item")[0]

        expect:
        statItem.getRel("scv:dimension").about == desc.expandCurie("dct:created")
        statItem.getValue("tl:atYear") == "2008"
    }

    def newDescriber(conn, String... contexts) {
        return new Describer(conn, false, contexts).
            setPrefix("dct", "http://purl.org/dc/terms/").
            setPrefix("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#").
            setPrefix("iana", "http://www.iana.org/assignments/relation/").
            setPrefix("foaf", "http://xmlns.com/foaf/0.1/").
            setPrefix("scv", "http://purl.org/NET/scovo#").
            setPrefix("event", "http://purl.org/NET/c4dm/event.owl#").
            setPrefix("tl", "http://purl.org/NET/c4dm/timeline.owl#")
    }

}
