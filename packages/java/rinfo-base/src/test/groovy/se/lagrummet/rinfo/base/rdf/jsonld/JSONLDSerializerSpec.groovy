package se.lagrummet.rinfo.base.rdf.jsonld

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer

import spock.lang.*

class JSONLDSerializerSpec extends Specification {

    static final FOAF = "http://xmlns.com/foaf/0.1/"
    def repo
    def describer

    def setup() {
        repo = RDFUtil.createMemoryRepository()
        describer = new Describer(repo.getConnection())
        describer.setPrefix("foaf", FOAF)
    }

    def cleanup() {
        describer.close()
    }

    def "should serialize RDF as JSON"() {
        // TODO: split into separate tests
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
        and:
        def personIRI = "http://example.org/person/someone#self"
        def homepageIRI = "http://example.org/person/someone"
        def person = describer.newDescription(personIRI, "foaf:Person")
        person.addLiteral("foaf:name", "Some One")
        def homepage = person.addRel("foaf:homepage", homepageIRI)
        homepage.addType("foaf:Document")

        when:
        def data = serializer.toJSON(repo, personIRI)
        then:
        data.iri == personIRI
        data.type == 'foaf:Person'
        data.name == "Some One"
        data.homepage.iri == homepageIRI
        data.homepage.type == 'Document'

        // NOTE: *experimental* @rev support
        when: "serializing homepage"
        def hpData = serializer.toJSON(repo, homepageIRI)
        and:
        def rev = hpData['@rev']
        then: "data should contain reverse relation to person"
        rev.homepage.size() == 1
        rev.homepage[0].iri == personIRI
        and: "roto resource in reverse item only contains subject"
        rev.homepage[0].homepage == [iri: homepageIRI]
    }

    def "should use default vocab"() {
        given:
        def context = [
            "@vocab": FOAF
        ]
        and:
        def topicIRI = "http://example.org/person/someone#self"
        def serializer = new JSONLDSerializer(context)
        describer.newDescription(topicIRI, "foaf:Person")
        when:
        def data = serializer.toJSON(repo, topicIRI)
        then:
        serializer.vocab == FOAF
        data['@type'] == 'Person'
    }

}
