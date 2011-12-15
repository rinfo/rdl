package se.lagrummet.rinfo.base.rdf.checker

import org.openrdf.model.URI
import org.openrdf.model.vocabulary.RDF
import org.openrdf.repository.RepositoryConnection

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

}
