package se.lagrummet.rinfo.service

import spock.lang.*

import org.apache.abdera.Abdera

import org.openrdf.model.vocabulary.RDF

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer


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


    RepoEntry repoEntry
    Describer desc

    def setup() {
        def repo = RDFUtil.createMemoryRepository()
        def conn = repo.connection
        SesameLoader loader = Mock()
        loader.getConn() >> conn
        loader.getResponseAsInputStream(_) >> {
            new ByteArrayInputStream(rdfBytes.getBytes("utf-8"))
        }
        repoEntry = new RepoEntry(loader, entry)
        desc = repoEntry.entryStats.doc.describer
        repoEntry.create()
    }

    def "should add entry for resource"() {
        given:
        def atomEntry = desc.subjects("foaf:primaryTopic", resourceUri)[0]
        expect:
        atomEntry.type.about == desc.expandCurie("awol:Entry")
        atomEntry.getString("awol:updated") == updated
        atomEntry.getRel("awol:content").about == "${resourceUri}.rdf"
        atomEntry.getRel("awol:content").getString("awol:type") == "application/rdf+xml"
    }

    def "should add statistics for resource"() {
        given:
        def statsDesc = repoEntry.entryStats.desc
        def statItem = statsDesc.getByType("scv:Item")[0]

        /*
        expect:
        statItem.getRel("scv:dimension").about == desc.expandCurie("dct:created")
        statItem.getString("tl:atYear") == "2008"
        */

        def dimensions = statItem.getRels("scv:dimension")

        expect:

        dimensions.size() == 3

        dimensions.find {
            it.getObjectUri("owl:onProperty") == "http://purl.org/dc/terms/created"
        }.getString("tl:atYear") == "2008"

        dimensions.find {
            it.getObjectUri("owl:onProperty") == statsDesc.expandCurie("rdf:type")
        }.getRel("owl:hasValue").about == statsDesc.expandCurie("rpubl:Myndighetsforeskrift")

        dimensions.find {
            it.getObjectUri("owl:onProperty") == statsDesc.expandCurie("dct:publisher")
        }.getRel("owl:hasValue").about == publisherUri

        statItem.getNative("rdf:value") == 1
        statItem.getRels("event:product").find { it.about == resourceUri }
    }

    def "should create dimension URI:s"() {
        given:
        def stats = repoEntry.entryStats
        def toUri = { stats.desc.expandCurie(it) }

        expect:
        stats.createDimensionLocalUri(
                toUri("dct:publisher"), toUri("rorg:regeringskansliet")) ==
            "dct:publisher+rorg:regeringskansliet"
    }


}
