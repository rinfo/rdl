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

    @Unroll("checking resource #name")
    def "problematic data"() {
        when:
        def report = checker.check(repo, "http://example.org/publ/${name}")
        then:
        def item = report.items[0]
        item.class.is failureType
        if (messageMatches)
            assert item.error.message =~ messageMatches
        report.items.size() == 1
        where:
        name                    | failureType                   | messageMatches
        "bad_uri"               | MalformedURIRefErrorItem      | /Illegal character in authority/
        "datatype_error"        | DatatypeErrorItem             | /Invalid value \d+ for \w+ field/
        "no_class"              | MissingTypeWarnItem           | null
        "undefined_class"       | UnknownTypeWarnItem           | null
        "undefined_property"    | UnknownPropertyWarnItem       | null
        "lang_expected_date"    | UnexpectedDatatypeErrorItem   | null
        "datatype_expected_lang"| ExpectedLangErrorItem         | null
        "expected_lang"         | ExpectedLangErrorItem         | null
        "spurious_whitespace"   | SpuriousWhiteSpaceWarnItem    | null
        "unexpected_uri_pattern"| PatternMismatchErrorItem      | null
        "improbable_future"     | PatternMismatchErrorItem      | null
        "improbable_past"       | PatternMismatchErrorItem      | null
    }

    def "ok data"() {
        given:
        def report = checker.check(repo, "http://example.org/publ/${name}")
        expect:
        report.items.size() == 0
        where:
        name << [
            "ok_two_titles",
        ]
    }
}
