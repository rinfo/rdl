package se.lagrummet.rinfo.service

import spock.lang.*

class ElasticLoaderSpec extends Specification {

    def "should clean json to fit ElasticSearch"() {
        expect:
        ElasticLoader.cleanForElastic(
                [key: ["item", true, [iri: "ref"]]]).key ==
                [iri: "ref"]
        ElasticLoader.cleanForElastic(
                [key: ["item", true, [iri: "ref1"], [iri: "ref2"]]]).key ==
                [[iri: "ref1"], [iri: "ref2"]]
        ElasticLoader.cleanForElastic(
                [iri: "ref", link: "text"], ['link']) == [iri: "ref"]
    }

}
