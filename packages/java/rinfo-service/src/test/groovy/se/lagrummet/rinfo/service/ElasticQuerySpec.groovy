package se.lagrummet.rinfo.service

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.SearchHit
import org.restlet.data.Reference

import se.lagrummet.rinfo.base.rdf.jsonld.JSONLDContext

import groovy.json.JsonSlurper
import spock.lang.*


class ElasticQuerySpec extends Specification {

    def elQuery = new ElasticQuery(
            new ElasticData(null, "es-index",
            new JsonLdSettings(new JSONLDContext([:]), [:]), "true"),
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

    def "should filter on date range queries"() {
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

    def "should filter on not exists"() {
        given:
        def srb = Mock(SearchRequestBuilder)
        def ref = new Reference("/-/publ?exists-references.iri=false")
        when:
        elQuery.prepareElasticSearch(srb, ref, "publ", showTerms)
        then:
        1 * srb.setQuery({
            def json = toJson(it)
            assert json.filtered?.filter?.not?.filter?.exists?.field == 'references.iri'
            true
        })
    }

    // TODO: ifExists

    def "should support OR:able query items"() {
        given:
        def ref = new Reference("/-/publ?q=thing&or-title=SFS&or-identifier=SFS")
        def srb = Mock(SearchRequestBuilder)
        def qs = null
        when:
        elQuery.prepareElasticSearch(srb, ref, "publ", showTerms)
        then:
        1 * srb.setQuery({ assert toJson(it).query_string.query ==
                "(thing) AND ((identifier:SFS) OR (title:SFS))"; true })
    }

    def "should support OR:able filter items"() {
        given:
        def srb = Mock(SearchRequestBuilder)
        def ref = new Reference("/-/publ?or-min-published=2008-01-01&or-min-issued=2008-01-01")
        when:
        elQuery.prepareElasticSearch(srb, ref, "publ", showTerms)
        then:
        1 * srb.setQuery({
            def f = toJson(it).filtered.filter
            assert f.or.filters[0].range.published.from == "2008-01-01"
            assert f.or.filters[1].range.issued.from == "2008-01-01"
            true
        })
    }


    //"should support OR:able combined query and filter items"?

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

    def "should add pagination info"() {

        given:
        def data = [itemsPerPage: 50, totalResults: 101]
        def qs = "label=*"
        when:
        def data1 = data + [startIndex: 0]
        elQuery.addPagination("publ", [page: 0, queryString: qs], 50, data1)
        then:
        data1.prev == null
        data1.current == "/-/publ?_page=0&label=*"
        data1.next == "/-/publ?_page=1&label=*"

        when:
        def data2 = data + [startIndex: 50]
        elQuery.addPagination("publ", [page: 1, queryString: qs], 50, data2)
        then:
        data2.prev == "/-/publ?_page=0&label=*"
        data2.current == "/-/publ?_page=1&label=*"
        data2.next == "/-/publ?_page=2&label=*"

        when:
        def data3 = data + [startIndex: 100]
        elQuery.addPagination("publ", [page: 2, queryString: qs], 1, data3)
        then:
        data3.prev == "/-/publ?_page=1&label=*"
        data3.current == "/-/publ?_page=2&label=*"
        data3.next == null

    }

    def "should build item from a hit"() {
        when:
        def item = elQuery.buildResultItem([
                getFields: {
                    [
                        "label": [value: "Item 1", values: ["Item 1"]],
                        "parts": [value: "Part 1", values: ["Part 1", "Part 2"]],
                        "rel.id": [value: "rel.1", values: ["rel.1"]],
                        "rel.label": [value: "Rel 1", values: ["Rel 1"]],
                    ]
                },
                getHighlightFields: {
                    [:]
                }
            ] as SearchHit)
        then:
        item.label == "Item 1"
        item.parts == ["Part 1", "Part 2"]
        item.rel.id == "rel.1"
        item.rel.label == "Rel 1"
    }

    private toJson(qb) {
        new JsonSlurper().parseText(qb.buildAsBytes().toUtf8())
    }

}
