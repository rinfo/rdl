package se.lagrummet.rinfo.base.rdf.jsonld

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer

import spock.lang.*


class JSONLDSerializerSpec extends Specification {

    static FOAF = "http://xmlns.com/foaf/0.1/"
    static XSD = JSONLDSerializer.XSD

    def repo
    def describer

    def personIRI = "http://example.org/person/someone#self"
    def homepageIRI = "http://example.org/person/someone"

    def setup() {
        repo = RDFUtil.createMemoryRepository()
        describer = new Describer(repo.getConnection())
        describer.setPrefix("foaf", FOAF)
        def person = describer.newDescription(personIRI, "foaf:Person")
        person.addLiteral("foaf:bday", "1970-01-01", "xsd:date")
        person.addLiteral("foaf:name", "Some One")
        def homepage = person.addRel("foaf:homepage", homepageIRI)
        homepage.addType("foaf:Document")
    }

    def cleanup() {
        describer.close()
    }

    def "should serialize RDF as JSON"() {
        given:
        def context = new JSONLDContext(
            iri: "@subject",
            type: "@type",
            foaf: FOAF,
            name: "${FOAF}name",
            homepage: "${FOAF}homepage",
            Document: "${FOAF}Document",
        )
        and:
        def serializer = new JSONLDSerializer(context)

        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.iri == personIRI
        data.type == 'foaf:Person'
        data.name == "Some One"
        data.homepage.iri == homepageIRI
        data.homepage.type == 'Document'
    }

    def "should use default vocab"() {
        given:
        def context = new JSONLDContext(
            "@vocab": FOAF
        )
        and:
        def serializer = new JSONLDSerializer(context)
        describer.newDescription(personIRI, "foaf:Person")
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        serializer.context.vocab == FOAF
        data['@type'] == 'Person'
    }

    def "should output explicit datatyped literals"() {
        given:
        def context = new JSONLDContext(
            xsd: XSD,
            "@vocab": FOAF,
            "bday": "${FOAF}bday"
        )
        and:
        def serializer = new JSONLDSerializer(context)
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.bday == ["@literal": "1970-01-01", "@datatype": "xsd:date"]
    }

    def "should support current coerce key"() {
        given:
        def context = new JSONLDContext(
            xsd: XSD,
            "@vocab": FOAF,
            "bday": "${FOAF}bday",
            "@coerce": ["bday": "xsd:date"]
        )
        and:
        def serializer = new JSONLDSerializer(context)
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.bday == "1970-01-01"
    }

    def "should support experimental combined form"() {
        given:
        def context = new JSONLDContext(
            xsd: XSD,
            "@vocab": FOAF,
            "bday": ["bday": "xsd:date"]
        )
        and:
        def serializer = new JSONLDSerializer(context)
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.bday == "1970-01-01"
    }

    // TODO: coerce @iri
    //          "@coerce": ["homepage": "@iri"]

    // TODO: coerce @list
    //          "@coerce": ["bibo:authorList": "@list"]

    def "should use experimental '@set' coerce to always make a list"() {
        given:
        def otherIRI = "http://example.org/person/other#self"
        def context = new JSONLDContext(
            knows: ["${FOAF}knows": "@set"]
        )
        def serializer = new JSONLDSerializer(context)
        describer.findDescription(personIRI).addRel("foaf:knows", otherIRI)
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.knows instanceof List
        data.knows[0]['@subject'] == otherIRI
    }

    def "should add both type token and type data if specified"() {
        given:
        def context = new JSONLDContext(
            "a": "@type",
            "type": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
            "@vocab": FOAF
        )
        and:
        def serializer = new JSONLDSerializer(context)
        describer.newDescription(personIRI, "foaf:Person")
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        serializer.context.vocab == FOAF
        data['a'] == 'Person'
        data['type']['@subject'] == "${FOAF}Person"
    }

    // TODO: coerce @rev...

    def "should support experimental '@rev' notation"() {
        given:
        def context = new JSONLDContext(
            homepage: "${FOAF}homepage"
        )
        def serializer = new JSONLDSerializer(context, false, true)
        when: "serializing homepage"
        def hpData = serializer.toJSON(repo, homepageIRI)
        and:
        def rev = hpData['@rev']
        then: "data should contain reverse relation to person"
        rev.homepage.size() == 1
        rev.homepage[0]['@subject'] == personIRI
        and: "roto resource in reverse item only contains subject"
        rev.homepage[0].homepage == ['@subject': homepageIRI]
    }

    def "should return null if missing data about resource"() {
        given:
        def context = new JSONLDContext("@vocab": FOAF)
        def serializer = new JSONLDSerializer(context)
        when:
        def data = serializer.toJSON(repo, "http://example.org/nothing")
        then:
        data == null
    }

    def "should return data even if only rev data about resource"() {
        given:
        def nullIRI = "http://example.org/null"
        def context = new JSONLDContext("@vocab": FOAF)
        def serializer = new JSONLDSerializer(context, false, true)
        describer.findDescription(personIRI).addRel("foaf:knows", nullIRI)
        when:
        def data = serializer.toJSON(repo, nullIRI)
        then:
        data['@rev']['knows'][0]['@subject'] == personIRI
    }

}
