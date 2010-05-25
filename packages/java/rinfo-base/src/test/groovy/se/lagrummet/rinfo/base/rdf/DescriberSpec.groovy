package se.lagrummet.rinfo.base.rdf

import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore

import spock.lang.*


class DescriberSpec extends Specification {

    def repo
    def conn

    static ORG_URI = "http://example.org"

    def setup() {
        repo = new SailRepository(new MemoryStore())
        repo.initialize()
        conn = repo.getConnection()
    }

    def cleanup() {
        conn.close()
        repo.shutDown()
    }

    def "should build and read rdf"() {
        given:
        def describer = newDescriber()

        when: "we make a lot of statements"
        def p = describer.newDescription("$ORG_URI/persons/some_body#person")
        p.addType("foaf:Person")
        p.addValue("foaf:name", "Some Body")
        p.addValue("foaf:firstName", "Some")
        p.addValue("foaf:surname", "Body")
        p.addRel("foaf:homepage", "$ORG_URI/persons/some_body")
        p.addValue("rdfs:comment", "I am somebody.", "@en")
        def img = p.addRel("foaf:depiction", "$ORG_URI/img/some_body.png")
        img.addType("foaf:Image")
        img.addRel("foaf:thumbnail", "$ORG_URI/img/some_body-64x64.png")
        def cv = p.addRev("cv:aboutPerson")
        cv.addType("cv:CV")
        def work1 = cv.addRel("cv:hasWorkHistory")
        work1.addValue("cv:startDate", "1999-01-01T00:00:00Z", "xsd:dateTime")
        work1.addRel("cv:employedIn", "http://example.com/about#")
        def work2 = cv.addRel("cv:hasWorkHistory")
        work2.addValue("cv:startDate", "2000-01-01T00:00:00Z", "xsd:dateTime")
        work2.addRel("cv:employedIn", "http://example.net/about#")
        //
        //def writer = new org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter(System.out)
        //def writer = new org.openrdf.rio.n3.N3Writer(System.out)
        //conn.exportStatements(null, null, null, false, writer)

        then: "we can find the added data"
        p.about == "http://example.org/persons/some_body#person"
        p.type.about == "http://xmlns.com/foaf/0.1/Person"
        p.getValue("foaf:name") == "Some Body"
        img = p.getRel("foaf:depiction")
        img.getRel("foaf:thumbnail").about == "http://example.org/img/some_body-64x64.png"
        cv = p.getRev("cv:aboutPerson")
        cv.getRels("cv:hasWorkHistory").collect { it.getRel("cv:employedIn").about }.sort() ==
                ["http://example.com/about#", "http://example.net/about#"]
    }

    def "should find subjects and objects"() {
        given:
        def describer = newDescriber()
        def s = "$ORG_URI/persons/some_body#person"
        def o = "$ORG_URI/img/some_body.png"

        when: "a relation is added"
        def p = describer.newDescription(s)
        p.addRel("foaf:depiction", o)

        then: "subject and object are found via the added relation"
        describer.subjectDescriptions("foaf:depiction", o).collect { it.about } == [s]
        describer.objectDescriptions(s, "foaf:depiction").collect { it.about } == [o]

        and: "any reference finds them"
        describer.subjectDescriptions(null, o).collect { it.about } == [s]
        describer.objectDescriptions(s, null).collect { it.about } == [o]

        and: "there is only one of each"
        describer.subjectDescriptions(null, null).collect { it.about } == [s]
        describer.objectDescriptions(null, null).collect { it.about } == [o]
    }

    def newDescriber() {
        return new Describer(conn).
            setPrefix('foaf', "http://xmlns.com/foaf/0.1/").
            setPrefix('cv', "http://purl.org/captsolo/resume-rdf/0.2/cv#")
    }

}
