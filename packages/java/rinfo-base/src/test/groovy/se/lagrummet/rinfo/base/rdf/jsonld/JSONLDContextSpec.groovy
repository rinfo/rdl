package se.lagrummet.rinfo.base.rdf.jsonld

import spock.lang.*


class JSONLDContextSpec extends Specification {

    static FOAF = "http://xmlns.com/foaf/0.1/"
    static XSD = JSONLDSerializer.XSD

    def "should support '@context' key"() {
        given:
        def context = new JSONLDContext("@context": [foaf: FOAF])
        expect:
        context.resolve("foaf") == FOAF
    }

    def "should support direct context content"() {
        given:
        def context = new JSONLDContext([foaf: FOAF])
        expect:
        context.resolve("foaf") == FOAF
    }

    def "should handle object definitions"() {
        given:
        def context = new JSONLDContext(foaf: FOAF, xsd: XSD,
            name: ["@id": "foaf:name", "@type": "xsd:string"],
            knows: ["@id": "foaf:knows", "@container": "@set"],
            about: ["@reverse": "foaf:isPrimaryTopicOf"])
        def name = context.keyTermMap["name"]
        def knows = context.keyTermMap["knows"]
        def about = context.keyTermMap["about"]
        expect:
        name.iri == "${FOAF}name"
        name.datatype == "${XSD}string"
        !name.isSet
        !name.isList
        and:
        knows.isSet
        !knows.isList
        !knows.isRev
        and:
        about.iri == "${FOAF}isPrimaryTopicOf"
        about.isRev
    }

    def "Should resolve refs"() {
        given:
        def context = new JSONLDContext(
            "@vocab": FOAF,
            foaf: FOAF,
        )
        expect:
        context.resolve("http://example.org/name") == "http://example.org/name"
        context.resolve("foaf:name") == "${FOAF}name"
        context.resolve("name") == "${FOAF}name"
    }

}
