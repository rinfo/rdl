package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.openrdf.model.Namespace
import org.openrdf.model.Resource
import org.openrdf.model.ValueFactory
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.XMLSchema
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.rio.RDFFormat
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore


import se.lagrummet.rinfo.base.atom.FeedArchiveReader
import se.lagrummet.rinfo.base.atom.AtomEntryDeleteUtil
import se.lagrummet.rinfo.base.rdf.RDFUtil


class SesameLoader extends FeedArchiveReader {

    Repository repository

    private final logger = LoggerFactory.getLogger(SesameLoader)

    static final AWOL_ENTRY
    static final AWOL_ID
    static final AWOL_UPDATED
    static {
        ValueFactory vf = ValueFactoryImpl.getInstance()
        def AWOL_NS = "http://bblfish.net/work/atom-owl/2006-06-06/#"
        AWOL_ENTRY = vf.createURI(AWOL_NS, "Entry")
        AWOL_ID = vf.createURI(AWOL_NS, "id")
        AWOL_UPDATED = vf.createURI(AWOL_NS, "updated")
    }


    SesameLoader(Repository repository) {
        this.repository = repository
    }

    boolean processFeedPage(URL pageUrl, Feed feed) {
        // TODO: store "last safe entry date" before reading, retry if still there!
        // (pageUrl, entry and date)
        def conn = repository.connection
        feed = feed.sortEntriesByUpdated(true)

        // FIXME: test use of deletedMap!
        def deletedMap = AtomEntryDeleteUtil.getDeletedMarkers(feed)

        for (URI uri : deletedMap.values()) {
            // TODO: error if not exists?
            /* TODO: find context..
            def context = vf.createURI(uri.toString())
            conn.clear(context)
            */
        }

        for (Entry entry : feed.entries) {

            def vf = repository.valueFactory
            def entryIdLiteral = vf.createLiteral(entry.id.toString(), XMLSchema.ANYURI)
            def entryUpdatedLiteral = RDFUtil.createDateTime(vf, entry.getUpdated())

            Resource storedContext = null
            def contextStmt = RDFUtil.one(conn, null, AWOL_ID, entryIdLiteral)
            if (contextStmt != null) {
                storedContext = contextStmt.subject
            }

            if (storedContext != null) {
                def storedUpdated = null
                def updatedStmt = RDFUtil.one(conn, storedContext, AWOL_UPDATED, null)
                if (updatedStmt != null) {
                    storedUpdated = updatedStmt.object
                }
                if (entryUpdatedLiteral.equals(storedUpdated)) {
                    logger.info("Encountered collected entry <${entry.getId()}>; stopping.")
                    return false
                } else {
                    conn.clear(storedContext)
                }
            }

            // TODO: add in which context (itself - i.e. context)?
            // *Must be removed/updated when the context is*!
            Resource context = vf.createBNode() // TODO: use getSelfLink URI (stable)?
            conn.add(context, RDF.TYPE, AWOL_ENTRY, context)
            conn.add(context, AWOL_ID, entryIdLiteral, context)
            conn.add(context, AWOL_UPDATED, entryUpdatedLiteral, context)

            // TODO: move to top of loop (when deleting from deletedMap is active)
            if (deletedMap.containsKey(entry.getId())) {
                logger.info("Deleting RDF from entry <${entry.id}>")
                continue
            }

            logger.info("Loading RDF from entry <${entry.id}>")
            Collection<ReprRef> rdfReferences = getRdfReferences(entry)
            for (ReprRef rdfRef : rdfReferences) {
                logger.info("RDF from <${rdfRef.url}>")
                loadData(conn, rdfRef, context)
            }

        }
        conn.close()

        // TODO: stop at feed with entry at minDateTime..
        //Date minDateTime=null
        return true
    }

    protected Collection<ReprRef> getRdfReferences(Entry entry) {
        Collection<ReprRef> rdfReferences =
                new ArrayList<ReprRef>()
        def contentElem = entry.contentElement
        def contentUrlPath = contentElem.resolvedSrc.toString()
        def contentMimeType = contentElem.mimeType.toString()
        contentUrlPath = unescapeColon( contentUrlPath )

        // TODO: manual RDFa-parsing?
        def rdfMimeType = "application/rdf+xml" // TODO: in "allowed" list?

        if (contentMimeType.equals(rdfMimeType)) {
            rdfReferences.add(new ReprRef(new URL(contentUrlPath), contentMimeType))
        }
        for (link in entry.links) {
            def urlPath = unescapeColon( link.resolvedHref.toString() )
            def mediaType = link.mimeType.toString()
            if (mediaType.equals(rdfMimeType)) {
                rdfReferences.add(new ReprRef(new URL(urlPath), mediaType))
            }
        }
        return rdfReferences
    }

    protected void loadData(RepositoryConnection conn, ReprRef repr, Resource context) {
        def inStream = getResponseAsInputStream(repr.url)
        conn.add(
                inStream, repr.url.toString(), RDFFormat.forMIMEType(repr.mediaType),
                context)
        conn.commit()
    }

}

class ReprRef {
    URL url
    String mediaType
    public ReprRef(URL url, String mediaType) {
        this.url = url
        this.mediaType = mediaType
    }
}
