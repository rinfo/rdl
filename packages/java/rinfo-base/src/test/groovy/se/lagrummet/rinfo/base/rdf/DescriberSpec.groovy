package se.lagrummet.rinfo.base.rdf

import org.openrdf.repository.RepositoryConnection
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
        def p = describer.newDescription("${ORG_URI}/persons/some_body#person")
        p.addType("foaf:Person")
        p.addValue("foaf:name", "Some Body")
        p.addValue("foaf:firstName", "Some")
        p.addValue("foaf:surname", "Body")
        p.addRel("foaf:homepage", "${ORG_URI}/persons/some_body")
        p.addValue("rdfs:comment", "I am somebody.", "@en")
        def img = p.addRel("foaf:depiction", "${ORG_URI}/img/some_body.png")
        img.addType("foaf:Image")
        img.addRel("foaf:thumbnail", "${ORG_URI}/img/some_body-64x64.png")
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
        //conn.export(writer)

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

    def "should find all by type"() {
        given:
        def describer = newDescriber()
        def personUris = [
            "${ORG_URI}/persons/some_body#person_1",
            "${ORG_URI}/persons/some_body#person_2"
        ]
        when:
        personUris.each {
            describer.newDescription(it).addType("foaf:Person")
        }
        then:
        describer.getByType("foaf:Person").collect { it.about }.sort() ==  personUris
    }

    def "should find subjects and objects"() {
        given:
        def describer = newDescriber()
        def s = "${ORG_URI}/persons/some_body#person"
        def o = "${ORG_URI}/img/some_body.png"

        and: "a relation"
        describer.newDescription(s).addRel("foaf:depiction", o)

        expect: "subject and object are found via the added relation"
        describer.subjects("foaf:depiction", o).collect { it.about } == [s]
        describer.objects(s, "foaf:depiction").collect { it.about } == [o]

        and: "any reference finds them"
        describer.subjects(null, o).collect { it.about } == [s]
        describer.objects(s, null).collect { it.about } == [o]

        and: "there is only one of each"
        describer.subjects(null, null).collect { it.about } == [s]
        describer.objects(null, null).collect { it.about } == [o]
    }

    def "should find subject URIs by value"() {
        given:
        def describer = newDescriber()
        def s = "${ORG_URI}/persons/some_body#person"
        def prop = "foaf:name"
        def v = "Some Body"
        and:
        describer.newDescription(s).addValue(prop, v)

        expect:
        describer.subjectUrisByValue(prop, v) == [s]
    }

    def "should created typed description"() {
        given:
        def describer = newDescriber()
        when:
        def p = describer.newDescription(null, "foaf:Person")
        then:
        p.type.about == "http://xmlns.com/foaf/0.1/Person"
    }

    def "should create blank description"() {
        given:
        def describer = newDescriber()
        when:
        def blank = describer.newDescription()
        then:
        blank.about.startsWith("_:")
    }

    def "should read primitive values"() {
        given:
        def describer = newDescriber()
        def p = describer.newDescription()
        p.addValue("foaf:age", value)
        expect:
        p.getLiteral("foaf:age").datatype == describer.expandCurie(datatype)
        where:
        value       | datatype
        true        | "xsd:boolean"
        12          | "xsd:int"
        1.2f        | "xsd:float"
        new Date()  | "xsd:dateTime"
    }

    def "should expose native values"() {
        given:
        def describer = newDescriber()
        def p = describer.newDescription()
        when:
        p.addValue("foaf:birthday", "1968-01-01T00:00:00Z", "xsd:dateTime")
        p.addValue("foaf:age", "42", "xsd:int")
        then:
        p.getNative("foaf:birthday") == new Date(-63158400000)
        p.getNative("foaf:age") == 42
    }

    def "should remove statements"() {
        given:
        def describer = newDescriber()
        def p = describer.newDescription()
        p.addValue("foaf:name", "Some Body")
        p.addRel("foaf:depiction", "${ORG_URI}/img/some_body.png")

        expect:
        p.getValue("foaf:name")
        p.getRel("foaf:depiction")

        when:
        p.remove("foaf:name")
        then:
        p.getValue("foaf:name") == null

        when:
        p.remove("foaf:depiction")
        then:
        p.getRel("foaf:depiction") == null

    }

    def "should only work in provided contexts"() {
        given:
        def ctx1 = "http://example.org/ctx/1"
        def ctx2 = "http://example.org/ctx/2"
        def about = "${ORG_URI}/persons/some_body#person"
        def name = "Some Body"
        def descriptionIn = { uri, ctx -> newDescriber(ctx).newDescription(uri) }

        when:
        descriptionIn(about, ctx1).addValue("foaf:name", name)
        then: "added data should be visible from this context"
        descriptionIn(about, ctx1).getValue("foaf:name") == name
        and: "not from any other context"
        descriptionIn(about, ctx2).getValue("foaf:name") == null
        and: "visible without given context"
        newDescriber().newDescription(about).getValue("foaf:name") == name

        when: "removing from other context"
        descriptionIn(about, ctx2).remove("foaf:name")
        then: "data should remain in first context"
        descriptionIn(about, ctx1).getValue("foaf:name") == name

        when: "removing from context"
        descriptionIn(about, ctx1).remove("foaf:name")
        then: "it is removed"
        descriptionIn(about, ctx1).getValue("foaf:name") == null
        and: "from everywhere"
        newDescriber().newDescription(about).getValue("foaf:name") == null

        when: "re-adding.."
        descriptionIn(about, ctx1).addValue("foaf:name", name)
        and: "removing without given context"
        newDescriber().newDescription(about).remove("foaf:name")
        then: "it is removed from all contexts"
        newDescriber().newDescription(about).getValue("foaf:name") == null
        and:
        descriptionIn(about, ctx1).getValue("foaf:name") == null
    }

    def "should require defined prefixes on use"() {
        given:
        def describer = new Describer(conn)
        def bnode = describer.newDescription(null)
        when:
        bnode.addValue("some:property", "value")
        then:
        thrown(DescriptionException)
    }

    def "should store prefixes if configured"() {
        given:
        RepositoryConnection conn = Mock()
        def FOAF = "http://xmlns.com/foaf/0.1/"

        when:
        new Describer(conn, true).setPrefix('foaf', FOAF)
        then:
        1 * conn.setNamespace('foaf', FOAF)

        when:
        new Describer(conn, false).setPrefix('foaf', FOAF)
        then:
        0 * conn.setNamespace('foaf', FOAF)
    }

    def "should close underlying connection"() {
        given:
        RepositoryConnection conn = Mock()
        def describer = new Describer(conn)
        when:
        describer.close()
        then:
        1 * conn.close()
    }

    def newDescriber(String... contexts) {
        return new Describer(conn, true, contexts).
            setPrefix('foaf', "http://xmlns.com/foaf/0.1/").
            setPrefix('cv', "http://purl.org/captsolo/resume-rdf/0.2/cv#")
    }

}
