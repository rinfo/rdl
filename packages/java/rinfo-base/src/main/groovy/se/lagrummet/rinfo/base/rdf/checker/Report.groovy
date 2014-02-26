package se.lagrummet.rinfo.base.rdf.checker

import org.openrdf.model.URI
import org.openrdf.model.vocabulary.RDF
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.RepositoryResult
import org.openrdf.model.Statement

class Report implements Closeable {

    RepositoryConnection connection
    URI errorType

    Report(RepositoryConnection conn, URI errorType) {
        this.connection = conn
        this.errorType = errorType
    }

    void close() {
        connection.close()
        connection.getRepository().shutDown()
    }

    boolean isEmpty() {
        connection.size() == 0
    }

    boolean isHasErrors() {
        return connection.hasStatement(null, RDF.TYPE, errorType, false)
    }

    List<Statement> getAllStatements() {
        def allStatements = new ArrayList<Statement>();
        RepositoryResult<Statement> result = connection.getStatements(null, null, null, false)
        try {
            while (result.hasNext()) {
                allStatements.add(result.next())
            }
        } finally {
            result.close()
        }

        return allStatements
    }
}
