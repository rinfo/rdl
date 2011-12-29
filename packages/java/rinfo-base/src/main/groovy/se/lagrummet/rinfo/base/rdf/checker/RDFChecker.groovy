package se.lagrummet.rinfo.base.rdf.checker

import org.openrdf.model.URI
import org.openrdf.model.impl.URIImpl
import org.openrdf.query.QueryLanguage
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.util.RDFInserter

import se.lagrummet.rinfo.base.rdf.RDFUtil


class RDFChecker {

    Repository repository
    List<String> testQueries
    URI errorType = new URIImpl("http://purl.org/net/schemarama#Error")

    RDFChecker() {
        this(RDFUtil.createMemoryRepository())
    }

    RDFChecker(Repository repository) {
        this.repository = repository
    }

    Report check(Repository data, resourceIri, contextIri=resourceIri) {
        def conn = repository.getConnection()
        def contextResource = conn.valueFactory.createURI(contextIri)
        def dataConn = data.getConnection()
        try {
            def inserter =  new RDFInserter(conn)
            inserter.enforceContext(contextResource)
            inserter.setPreserveBNodeIDs(false)
            dataConn.export(inserter)
        } finally {
            dataConn.close()
        }
        def report = createReport(conn)
        try {
            conn.clear(contextResource)
        } finally {
            conn.close()
        }
        return report
    }

    Report createReport(RepositoryConnection conn) {
        def reportRepo = RDFUtil.createMemoryRepository()
        def reportConn = reportRepo.getConnection()
        for (query in testQueries) {
            def prepQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, query)
            prepQuery.evaluate(new RDFInserter(reportConn))
        }
        return new Report(reportConn, errorType)
    }

}
