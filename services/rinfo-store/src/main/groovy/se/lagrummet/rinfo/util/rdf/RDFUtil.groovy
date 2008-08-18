package se.lagrummet.rinfo.util.rdf

import org.openrdf.model.Resource
import org.openrdf.model.Statement
import org.openrdf.model.URI
import org.openrdf.model.Value
import org.openrdf.model.ValueFactory
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.RepositoryException
import org.openrdf.repository.sail.SailRepository
import org.openrdf.rio.RDFFormat
import org.openrdf.rio.RDFWriterRegistry
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter
import org.openrdf.sail.memory.MemoryStore


class RDFUtil {

    // Repo-level operations

    static Repository createMemoryRepository() {
        def r = new SailRepository(new MemoryStore())
        r.initialize()
        return r
    }

    static void loadDataFromURL(
            Repository repo, URL url, String mimeType=null) {
        def conn = url.openConnection()
        conn.setRequestProperty("Accept", mimeType)
        conn.connect()
        def stream = conn.getInputStream()
        try {
            loadDataFromStream(repo, stream, url.toString(), mimeType)
        } finally {
            stream.close()
        }
    }

    static void loadDataFromFile(
            Repository repo, File file, String mediaType=null) {
        if (mediaType == null) {
            mediaType = RDFFormat.forFileName(file.name).defaultMIMEType
        }
        loadDataFromStream(repo,
                new FileInputStream(file), file.toURI().toString(),
                mediaType)
    }

    static void loadDataFromStream(Repository repo,
            InputStream stream, String baseUri, String mimeType) {
        def parser = null
        // TODO: more formats, e.g. RDFa (opt. guess from url?)
        def format = RDFFormat.forMIMEType(mimeType)

        def conn = repo.connection
        try {
            conn.autoCommit = false
            conn.add(stream, baseUri, format)
            conn.commit()
        } catch (RepositoryException e) {
            conn.rollback()
        } finally {
            conn.close()
        }
    }

    static addToRepo(targetRepo, repoToAdd) {
        def targetConn = targetRepo.connection
        def connToAdd = repoToAdd.connection
        targetConn.add(connToAdd.getStatements(null, null, null, false))
    }

    static void addFile(Repository repo, String fpath, RDFFormat format) {
        def file = new File(fpath)
        String baseUri = file.toURI()
        def conn = repo.connection
        conn.add(file, baseUri, format)
        conn.commit()
    }

    static void serialize(
            Repository repo, String mimeType, OutputStream outStream) {
        def format = RDFFormat.forMIMEType(mimeType)
        def writer
        if (format == RDFFormat.RDFXML) {
            writer = new RDFXMLPrettyWriter(outStream)
        } else {
            def factory = RDFWriterRegistry.instance.get(format)
            writer = factory.getWriter(outStream)
        }
        def conn = repo.connection
        conn.exportStatements(null, null, null, false, writer)
        conn.close()
        //writer.close()
    }

    static InputStream serializeAsInputStream(
            Repository repo, String mimeType) {
        def outStream = new ByteArrayOutputStream()
        RDFUtil.serialize(repo, mimeType, outStream)
        outStream.close()
        return new ByteArrayInputStream(outStream.toByteArray())
    }

    // Statement-level operations
    // NOTE: GraphUtil looked promising, but Graph:s aren't prominent in
    // Sesame 2 (as in hard to create, disconnected from repo etc)

    static Statement one(
            Repository repo, Resource s, URI p, Value o,
            includeInferred=false) {
        def conn = repo.connection
        def stmts = conn.getStatements(s, p, o, includeInferred)
        def st = null
        while (stmts.hasNext()) {
            st = stmts.next()
        }
        stmts.close()
        conn.close()
        return st
    }



    // Idomatic operations..

    static Repository replaceURI(Repository repo,
            java.net.URI oldUri,
            java.net.URI newUri) {
        return replaceURI(repo, oldUri, newUri, false)
    }

    static Repository replaceURI(Repository repo,
            java.net.URI oldUri,
            java.net.URI newUri,
            replacePredicates) {
        def vf = repo.valueFactory
        return replaceURI(repo,
                vf.createURI(oldUri.toString()),
                vf.createURI(newUri.toString()),
                replacePredicates)
    }

    static Repository replaceURI(Repository repo,
            URI oldUri,
            URI newUri) {
        return replaceURI(repo, oldUri, newUri, false)
    }

    static Repository replaceURI(Repository repo,
            URI oldUri, URI newUri,
            replacePredicates) {

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

        def stmts = repoConn.getStatements(null, null, null, true)
        while (stmts.hasNext()) {
            def st = stmts.next()
            def subject = st.subject
            def predicate = st.predicate
            def object = st.object
            if (subject instanceof URI) {
                subject = changeURI(vf, subject, oldUri, newUri)
            }
            if (replacePredicates) {
                predicate = changeURI(vf, predicate, oldUri, newUri)
            }
            if (object instanceof URI) {
                object = changeURI(vf, object, oldUri, newUri)
            }

            newRepoConn.add(subject, predicate, object)
        }
        stmts.close()
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

}
