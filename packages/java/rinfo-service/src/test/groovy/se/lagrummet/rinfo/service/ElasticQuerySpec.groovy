package se.lagrummet.rinfo.service

import org.elasticsearch.client.action.search.SearchRequestBuilder
import org.restlet.data.Reference

import groovy.json.JsonSlurper
import spock.lang.*


class ElasticQuerySpec extends Specification {

    def elQuery = new ElasticQuery(
            new ElasticData(null, "es-index", new JsonLdSettings()),
            "http://service.lagrummet.se/")

    def showTerms = ['title', 'identifier'] as Set

    def "should build query"() {
        given:
        def ref = new Reference("/-/publ?q=Some+thing&identifier=SFS")
        def srb = Mock(SearchRequestBuilder)
        def qs = null
        when:
        elQuery.prepareElasticSearch(srb, ref, "publ", showTerms)
        then:
        1 * srb.setTypes("publ")
        1 * srb.addFields({ it == showTerms as String[] })
        1 * srb.setQuery({ assert toJson(it).query_string.query ==
                "(Some thing) AND (identifier:SFS)"; true })
    }

    def "should date range queries"() {
        given:
        def srb = Mock(SearchRequestBuilder)
        def ref = new Reference("/-/publ?min-updated=2008-01-01")
        when:
        elQuery.prepareElasticSearch(srb, ref, "publ", showTerms)
        then:
        1 * srb.setQuery({
            def r = toJson(it).filtered.filter.range.updated
            assert r.from == "2008-01-01"
            assert r.to == null
            true
        })
    }

    private toJson(qb) {
        new JsonSlurper().parseText(new String(qb.buildAsBytes()))
    }

    @Unroll
    def "can escape queries"() {
        expect:
        elQuery.escapeQueryString(qs) == esc
        where:
        qs              | esc
        /1999:175/      | /1999\:175/
        /1999:175)/     | /1999\:175\)/
        /a AND b AND/   | /a AND b /
        /a OR b OR/     | /a OR b /
        /AND b AND c/   | / b AND c/
        /OR b OR c/     | / b OR c/
    }

    @Unroll
    def "can turn a rinfo IRI to a service IRI"() {
        expect:
        elQuery.makeServiceLink(iri) == expected
        where:
        iri << [
            "http://rinfo.lagrummet.se/publ/sfs/199:175",
            "http://rinfo.lagrummet.se/publ/sfs/199:175#p1",
        ]
        expected << [
            "http://service.lagrummet.se/publ/sfs/199:175/data.json",
            "http://service.lagrummet.se/publ/sfs/199:175/data.json#p1",
        ]
    }

}
