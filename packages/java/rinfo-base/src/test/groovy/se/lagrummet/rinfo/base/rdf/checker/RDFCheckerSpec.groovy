package se.lagrummet.rinfo.base.checker

import se.lagrummet.rinfo.base.rdf.RDFUtil

import spock.lang.*


class RDFCheckerSpec extends Specification {

    @Shared checker
    @Shared repo

    def setupSpec() {
        repo = RDFUtil.slurpRdf("src/test/resources/rdf/checker/multiple_problems.ttl")
        checker = new RDFChecker()
        def inStream = getClass().getResourceAsStream("/rdf/checker/config.json")
        try {
            checker.schemaInfo.loadConfig(inStream)
        } finally {
            inStream.close()
        }
    }


    def "should be configured"() {
        expect:
        checker.schemaInfo.propertyMap
    }

    @Unroll("checking resource #id")
    def "should check rdf"() {
        when:
        def report = checker.check(repo, "http://example.org/publ/${id}")
        then:
        report.items.size() == 1
        def item = report.items[0]
        item.class.is failureType
        if (messageMatches)
            assert item.error.message =~ messageMatches
        where:
        id      | failureType                   | messageMatches
        1       | MalformedURIRefErrorItem      | /Illegal character in authority/
        2       | DatatypeErrorItem             | /Invalid value \d+ for \w+ field/
        3       | MissingTypeWarnItem           | null
        7       | SpuriousWhiteSpaceWarnItem    | null
    }

}
