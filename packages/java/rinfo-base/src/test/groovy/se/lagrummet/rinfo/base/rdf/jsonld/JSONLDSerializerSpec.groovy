package se.lagrummet.rinfo.base.rdf.jsonld

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer

import spock.lang.*

class JSONLDSerializerSpec extends Specification {

    static final FOAF = "http://xmlns.com/foaf/0.1/"
    def repo
    def describer

    def personIRI = "http://example.org/person/someone#self"
    def homepageIRI = "http://example.org/person/someone"

    def setup() {
        repo = RDFUtil.createMemoryRepository()
        describer = new Describer(repo.getConnection())
        describer.setPrefix("foaf", FOAF)
        def person = describer.newDescription(personIRI, "foaf:Person")
        person.addLiteral("foaf:name", "Some One")
        def homepage = person.addRel("foaf:homepage", homepageIRI)
        homepage.addType("foaf:Document")
    }

    def cleanup() {
        describer.close()
    }

    def "should serialize RDF as JSON"() {
        given:
        def context = [
            iri: "@subject",
            type: "@type",
            foaf: "${FOAF}",
            name: "${FOAF}name",
            homepage: "${FOAF}homepage",
            Document: "${FOAF}Document",
            "@coerce": [
                "@iri": ['homepage']
            ]
        ]
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

    def "should support experimental @rev notation"() {
        given:
        def context = [
            homepage: "${FOAF}homepage"
        ]
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

    def "should use default vocab"() {
        given:
        def context = [
            "@vocab": FOAF
        ]
        and:
        def serializer = new JSONLDSerializer(context)
        describer.newDescription(personIRI, "foaf:Person")
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        serializer.vocab == FOAF
        data['@type'] == 'Person'
    }

    def "should add both type token and type data if specified"() {
        given:
        def context = [
            "a": "@type",
            "type": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
            "@vocab": FOAF
        ]
        and:
        def serializer = new JSONLDSerializer(context)
        describer.newDescription(personIRI, "foaf:Person")
        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        serializer.vocab == FOAF
        data['a'] == 'Person'
        data['type']['@subject'] == "${FOAF}Person"
    }

}
