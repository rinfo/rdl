package se.lagrummet.rinfo.service

import spock.lang.*

import org.openrdf.model.vocabulary.RDF
import org.apache.abdera.Abdera
import se.lagrummet.rinfo.base.rdf.RDFUtil


class RepoEntrySpec extends Specification {

    def "should add eventItem for resource"() {

        given:
        def repo = RDFUtil.createMemoryRepository()
        def conn = repo.connection
        def loader = Mock(SesameLoader)
        loader.getConn() >> conn
        loader.getResponseAsInputStream(_) >> {
            new ByteArrayInputStream("""
        <rdf:Description
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:dct="http://purl.org/dc/terms/"
                rdf:about="http://example.org/things/1">
            <dct:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date"
                        >2008-10-08</dct:created>
        </rdf:Description>
            """.getBytes("utf-8"))
        }
        def entry = Abdera.instance.parser.parse(
            new ByteArrayInputStream("""
        <entry xmlns="http://www.w3.org/2005/Atom">
            <id>http://example.org/things/1</id>
            <updated>2008-10-10T20:00:00.000Z</updated>
            <title type="text"></title>
            <content type="application/rdf+xml"
                     src="http://example.org/things/1.rdf"/>
        </entry>
            """.getBytes("utf-8"))).root

        when:
        def repoEntry = new RepoEntry(loader, entry)
        repoEntry.create()

        then:
        def eventItem = RDFUtil.one(conn, null, RDF.TYPE, RepoEntry.SCOVO_ITEM).subject
        "http://purl.org/dc/terms/created" == RDFUtil.one(conn, eventItem,
                RepoEntry.SCOVO_DIMENSION, null).object.stringValue()
        "2008" == RDFUtil.one(conn, eventItem,
                RepoEntry.TL_AT_YEAR, null).object.stringValue()
    }

}
