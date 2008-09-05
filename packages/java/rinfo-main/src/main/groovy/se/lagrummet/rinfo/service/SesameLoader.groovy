package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.rio.RDFFormat
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.base.atom.FeedArchiveReader
import se.lagrummet.rinfo.base.rdf.RDFUtil


class SesameLoader extends FeedArchiveReader {

    Repository repository

    private final logger = LoggerFactory.getLogger(SesameLoader)

    SesameLoader(Repository repository) {
        this.repository = repository
    }

    boolean processFeedPage(URL pageUrl, Feed feed) {
        def conn = repository.connection
        for (entry in feed.entries) {
            def entryId = entry.id

            // TODO: from content *or* alternate recognized as RDF
            def rdfPath = entry.contentSrc.toString()
            rdfPath = unescapeColon(rdfPath)

            logger.info "Entry <${entryId}>, RDF from <${rdfPath}>"

            loadDataFromURL(conn, new URL(rdfPath), "${entryId}")
            // TODO: Check for tombstones; delete..: conn.clear(context)

        }
        conn.close()

        // TODO: stop at feed with entry at minDateTime..
        //Date minDateTime=null
        return true
    }

    void loadDataFromURL(RepositoryConnection conn, URL url, String uri) {
        // TODO: def contextUri = repository.valueFactory.createURI(uri + "#context")
        conn.add(url, url.toString(), RDFFormat.RDFXML)
        conn.commit()
    }

}
