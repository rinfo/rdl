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
        person.addRel("rdfs:seeAlso", homepageIRI)
        homepage.addType("foaf:Document")
    }

    def cleanup() {
        describer.close()
    }

    def "should serialize RDF as JSON"() {
        given:
        def context = new JSONLDContext(
            iri: "@id",
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
        data.bday == ["@value": "1970-01-01", "@type": "xsd:date"]
    }

    def "should use term as IRI if defined"() {
        given:
        def docTypeIri = "${FOAF}Document"
        def context = new JSONLDContext('Document': docTypeIri)
        def serializer = new JSONLDSerializer(context)
        and:
        describer.newDescription(docTypeIri).addRel("rdf:type", "rdfs:Class")
        when:
        def data = serializer.toJSON(repo, docTypeIri)
        then:
        data['@id'] == 'Document'
    }

    def "should support type coercion"() {
        given:
        def context = new JSONLDContext(
            xsd: XSD,
            "@vocab": FOAF,
            "bday": ["@id": "${FOAF}bday", "@type": "xsd:date"]
        )
        and:
        def serializer = new JSONLDSerializer(context)
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.bday == "1970-01-01"
    }

    // TODO: coerce @iri
    //          "@coerce": ["@id": "homepage", "@type": "@iri"]

    // TODO: coerce @list
    //          "@coerce": ["@id": "bibo:authorList", "@type": "@list"]

    def "should use '@set' to always make an array"() {
        given:
        def otherIRI = "http://example.org/person/other#self"
        def context = new JSONLDContext(
            knows: ["@id": "${FOAF}knows", "@container": "@set"]
        )
        def serializer = new JSONLDSerializer(context)
        describer.findDescription(personIRI).addRel("foaf:knows", otherIRI)
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.knows instanceof List
        data.knows[0]['@id'] == otherIRI
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
        data['type']['@id'] == "Person"
    }

    // TODO: coerce @reverse...

    def "should support experimental '@reverse' notation"() {
        given:
        def context = new JSONLDContext(
            homepage: "${FOAF}homepage"
        )
        def serializer = new JSONLDSerializer(context, false, true)
        when: "serializing homepage"
        def hpData = serializer.toJSON(repo, homepageIRI)
        and:
        def rev = hpData['@reverse']
        then: "data should contain reverse relation to person"
        rev.homepage.size() == 1
        rev.homepage[0]['@id'] == personIRI
        and: "roto resource in reverse item only contains subject"
        rev.homepage[0].homepage == ['@id': homepageIRI]
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
        data['@reverse']['knows'][0]['@id'] == personIRI
    }

    def "should create copy of data linked with different terms"() {
        given:
        def context = new JSONLDContext("foaf": FOAF, "rdfs": Describer.RDFS_NS)
        and:
        def serializer = new JSONLDSerializer(context, false, true)
        when:
        def person = serializer.toJSON(repo, personIRI)
        then:
        person['@id'] == personIRI
        person['foaf:homepage']['@id'] == homepageIRI
        person['rdfs:seeAlso']['@id'] == homepageIRI
        when:
        def homepage = serializer.toJSON(repo, homepageIRI)
        then:
        homepage['@id'] == homepageIRI
        homepage['@reverse']['foaf:homepage']?.getAt(0)?.get('@id') == personIRI
        homepage['@reverse']['rdfs:seeAlso']?.getAt(0)?.get('@id') == personIRI
    }

}
