package se.lagrummet.rinfo.base.rdf.checker

import org.openrdf.repository.RepositoryConnection

class Report implements Closeable {

    RepositoryConnection connection

    Report(RepositoryConnection conn) {
        this.connection = conn
    }

    void close() {
        connection.close()
        connection.getRepository().shutDown()
    }

    boolean isEmpty() {
        connection.size() == 0
    }

}
