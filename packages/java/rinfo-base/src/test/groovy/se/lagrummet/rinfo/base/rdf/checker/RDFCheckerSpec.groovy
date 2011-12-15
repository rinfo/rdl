package se.lagrummet.rinfo.base.rdf.checker

import org.openrdf.model.vocabulary.RDF
import org.openrdf.query.QueryLanguage
import se.lagrummet.rinfo.base.rdf.RDFUtil

import spock.lang.*


class RDFCheckerSpec extends Specification {

    @Shared checker

    static baseDir = new File("../../../resources/base/validation")

    static baseData = [
        //"rpubl.n3", "datasets.ttl"
        "../../../resources/external/rdf/rdf.rdfs",
        "../../../resources/external/rdf/dcterms.rdfs",
        "../../../resources/external/rdf/foaf.owl",
        new File(baseDir, "tests/schema.ttl").path
    ]

    static namedTests = [
        "iri_error",
        "datatype_error",
        "no_class",
        "unknown_class",
        "unknown_property",
        "missing_expected",
        "expected_datatype",
        "expected_lang",
        "unexpected_uri_pattern",
        "from_future",
        "improbable_future",
        "improbable_past",
        "spurious_whitespace"
    ]

    def setupSpec() {
        checker = new RDFChecker()
        baseData.each {
            RDFUtil.loadDataFromFile(checker.repository, new File(it))
        }
        def queries = namedTests.collect {
            new File(baseDir, "${it}.rq").text
        }
        checker.setTestQueries(queries)
    }

    @Unroll({"checking resource $name"})
    def "problematic data"() {
        given:
        def iri = "http://example.org/publ/${name}"
        def file = new File(baseDir, "tests/${name}.ttl")
        def data = RDFUtil.createMemoryRepository()
        RDFUtil.loadDataFromFile(data, file)
        when:
        def report = checker.check(data, iri)
        def result = queryToResultList(report.connection, """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX sch: <http://purl.org/net/schemarama#>
                SELECT * {
                    ?failure a ?type;
                        rdfs:isDefinedBy ?failedTest;
                        sch:implicated [ rdf:first ?failed ] .
                    FILTER(?type in (sch:Error, sch:Warning))
                }""", ['failed', 'failedTest'])

        then:

        !report.empty

        if (result.size() != 1) {
            println result
            dumpData(name, report.connection)
        }
        result.size() == 1

        where:
        name << namedTests
    }

    def "ok data"() {
        def iri = "http://example.org/publ/ok_two_titles"
        def report = null
        def file = new File(baseDir, "tests/ok_two_titles.ttl")
        def data = RDFUtil.createMemoryRepository()
        RDFUtil.loadDataFromFile(data, file)
        when:
        report = checker.check(data, iri)
        def conn = report.getConnection()
        then:
        conn.size() == 0
    }

    def "report properties"() {
        given:
        def repo = RDFUtil.createMemoryRepository()
        def conn = repo.getConnection()
        def vf = conn.valueFactory
        def report = new Report(conn, checker.errorType)
        expect:
        report.empty
        ! report.hasErrors
        when:
        conn.add(vf.createBNode(), RDF.TYPE, vf.createBNode())
        then:
        ! report.empty
        ! report.hasErrors
        when:
        conn.add(vf.createBNode(), RDF.TYPE, checker.errorType)
        then:
        ! report.empty
        report.hasErrors
    }

    private queryToResultList(conn, query, bindings) {
        def tupleQuery = conn.prepareTupleQuery(
                QueryLanguage.SPARQL, query)
        def result = tupleQuery.evaluate()
        def items = []
        while (result.hasNext()) {
            def bindingSet = result.next()
            items << bindings.collect { bindingSet.getValue(it) }
        }
        return items
    }

    private dumpData(name, conn) {
        println "=" * 40
        println name
        println "=" * 40
        RDFUtil.serialize(conn, RDFUtil.TURTLE, System.out, true)
        println()
        println()
    }

}
