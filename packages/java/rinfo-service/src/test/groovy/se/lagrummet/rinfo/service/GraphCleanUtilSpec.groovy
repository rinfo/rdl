package se.lagrummet.rinfo.service

import org.openrdf.query.QueryLanguage
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.rio.RDFFormat
import spock.lang.*
import se.lagrummet.rinfo.base.rdf.RDFUtil


class GraphCleanUtilSpec extends Specification {
    @Shared Repository repo
    @Shared RepositoryConnection conn

    def setupSpec() {
        repo = RDFUtil.createMemoryRepository()
        conn = repo.connection

        conn.add(getClass().getResourceAsStream(
                '/NamedGraphTestData.trig'), "http://rinfo.lagrummet.se/base", RDFFormat.TRIG)
        conn.commit()
        conn.close()
    }

    def 'should find subjects with where there are unwanted duplicates'() {
        given:
        when:
            def withDuplicates = GraphCleanUtil.subjectsWithManyPredicate(repo, "http://purl.org/dc/terms/title")
        then:
            withDuplicates.any()
            withDuplicates.size() == 1
    }
    def 'should find selected data from named graphs'() {
        given:
        when:
            def fromNamedGraph = GraphCleanUtil.tryGetDataFromNamedGraph(repo, "http://rinfo.lagrummet.se/publ/sfs/1999:175",
                    "http://purl.org/dc/terms/title", "http://rinfo.lagrummet.se/publ/sfs/1999:175/entry#context")
        then:
            fromNamedGraph == "\"Rättsinformationsförordning (1999:175)\"@sv"
    }
    def 'should be able to update graph'() {
        given:
        when:
            repo = GraphCleanUtil.updateGraph(repo, "http://rinfo.lagrummet.se/publ/sfs/1999:175",
                    "http://purl.org/dc/terms/title", "newTitle")
        then:
            def conn = repo.getConnection()
            def query = conn.prepareTupleQuery(QueryLanguage.SPARQL, "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "PREFIX owl:  <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX void: <http://rdfs.org/ns/void#>\n" +
                    "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX bibo: <http://purl.org/ontology/bibo/>\n" +
                    "PREFIX rpubl: <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#>\n" +
                    "SELECT * WHERE { ?s dct:title \"newTitle\" }")
            def result = query.evaluate()

            result.hasNext()
    }

    def 'should be able to filter repo'() {
        given:
            setupSpec()
        when:
            repo = GraphCleanUtil.filterRepo(repo, repo,"http://purl.org/dc/terms/title", "http://rinfo.lagrummet.se/publ/sfs/1999:175")
        then:
            GraphCleanUtil.subjectsWithManyPredicate(repo, "http://purl.org/dc/terms/title").size() == 0
    }

    def 'should be able to pick from consolidated'() {
        given:
            setupSpec()
        when:
            def uri = GraphCleanUtil.tryGetConsolidated(repo, "http://rinfo.lagrummet.se/publ/sfs/1999:175")
        then:
            uri == "http://rinfo.lagrummet.se/publ/sfs/1999:175/konsolidering/2011-05-02"
    }
}
