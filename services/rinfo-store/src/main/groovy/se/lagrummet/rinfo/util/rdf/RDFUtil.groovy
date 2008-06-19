package se.lagrummet.rinfo.util.rdf

import org.openrdf.model.Statement
import org.openrdf.model.URI
import org.openrdf.model.ValueFactory
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.RepositoryException
import org.openrdf.repository.sail.SailRepository
import org.openrdf.rio.RDFFormat
import org.openrdf.rio.rdfxml.RDFXMLParser
import org.openrdf.sail.memory.MemoryStore


class RDFUtil {

    static Repository replaceURI(Repository repo,
            URI oldUri, URI newUri,
            replacePredicates=false) {

        def repoConn = repo.connection
        def newRepo = createMemoryRepository()
        def newRepoConn = newRepo.connection

        def nsIter = repoConn.namespaces
        while (nsIter.hasNext()) {
            def ns = nsIter.next()
            newRepoConn.setNamespace(ns.prefix, ns.name)
        }
        nsIter.close()

        def vf = newRepo.valueFactory

        def stmtIter = repoConn.getStatements(null, null, null, true)
        while (stmtIter.hasNext()) {
            def stmt = stmtIter.next()

            def subject = changeURI(vf, stmt.subject, oldUri, newUri)
            def predicate = stmt.predicate
            def object = stmt.object
            if (replacePredicates) {
                predicate = changeURI(vf, predicate, oldUri, newUri)
            }
            if (object instanceof URI) {
                object = changeURI(vf, object, oldUri, newUri)
            }

            newRepoConn.add(subject, predicate, object)
        }
        stmtIter.close()
        repoConn.close()
        newRepoConn.close()
        return newRepo
    }

    static URI changeURI(ValueFactory vf, URI uri, URI oldUri, URI newUri) {
        def uriStr = uri.toString()
        if (!uriStr.startsWith(oldUri.toString())) {
            return uri
        }
        uriStr = uriStr.replaceFirst(oldUri.toString(), newUri.toString())
        return vf.createURI(uriStr)
    }

    static Repository createMemoryRepository() {
        def r = new SailRepository(new MemoryStore())
        r.initialize()
        return r
    }

    static loadDataFromURL(Repository repository, URL url, String mimeType) {
        def conn = url.openConnection()
        conn.setRequestProperty("Accept", mimeType)
        conn.connect()
        def stream = conn.getInputStream()
        try {
            loadDataFromStream(repository, stream, url.toString(), mimeType)
        } finally {
            stream.close()
        }
    }

    static void loadDataFromStream(Repository repository,
            InputStream stream, String baseUri, String mimeType) {
        def parser = null
        // TODO: more formats, e.g. RDFa (opt. guess from url?)
        assert mimeType == "application/rdf+xml"
        parser = new RDFXMLParser(repository.valueFactory)

        def conn = repository.connection
        try {
            conn.autoCommit = false
            parser.parse(stream, baseUri)
            conn.commit()
        } catch (RepositoryException e) {
            conn.rollback()
        } finally {
            conn.close()
        }
    }

}
