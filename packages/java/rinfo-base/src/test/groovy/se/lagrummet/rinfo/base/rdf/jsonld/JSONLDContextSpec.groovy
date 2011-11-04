package se.lagrummet.rinfo.base.rdf.jsonld

import spock.lang.*


class JSONLDContextSpec extends Specification {

    static FOAF = "http://xmlns.com/foaf/0.1/"

    def "should support explicit '@context' key"() {
        given:
        def ctxt = new JSONLDContext("@context": [foaf: FOAF])
        expect:
        ctxt.resolve("foaf") == FOAF
    }

    def "should support no '@context' key"() {
        // TODO: make failing test if this support is removed from spec.
        given:
        def ctxt = new JSONLDContext([foaf: FOAF])
        expect:
        ctxt.resolve("foaf") == FOAF
    }

    def "Should resolve refs"() {
        given:
        def ctxt = new JSONLDContext(
            "@vocab": FOAF,
            foaf: FOAF,
        )
        expect:
        ctxt.resolve("http://example.org/name") == "http://example.org/name"
        ctxt.resolve("foaf:name") == "${FOAF}name"
        ctxt.resolve("name") == "${FOAF}name"
    }

}
