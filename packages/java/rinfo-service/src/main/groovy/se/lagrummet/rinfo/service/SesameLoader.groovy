package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.openrdf.model.Literal
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


    SesameLoader(Repository repository) {
        this.repository = repository
    }

    boolean processFeedPage(URL pageUrl, Feed feed) {
        // TODO:? store "last safe entry date" before reading, retry if still there!
        // (pageUrl, entry and date)
        def conn = repository.connection
        feed = feed.sortEntriesByUpdated(true)

        def deletedMap = AtomEntryDeleteUtil.getDeletedMarkers(feed)

        for (Map.Entry<URI, Date> delItem : deletedMap.entrySet()) {
            def entryRepoData = new EntryRepoData(
                    delItem.getKey().toURI(), delItem.getValue().getDate(),
                    conn)
            logger.info("Deleting RDF from entry <${entryRepoData.id}>")
            entryRepoData.clearContext() // TODO: error if not exists?
            // TODO:? just add the tombstone as a marker (for collect) like this?
            entryRepoData.addContext()
        }

        for (Entry entry : feed.entries) {
            if (deletedMap.containsKey(entry.id)) {
                continue
            }
            def entryRepoData = new EntryRepoData(entry.id.toURI(), entry.updated, conn)
            if (entryRepoData.isCollected()) {
                logger.info("Encountered collected entry <${entry.getId()}>; stopping.")
                return false
            } else {
                if (entryRepoData.getStoredContext() != null) {
                    entryRepoData.clearContext()
                }
            }
            entryRepoData.addContext()
            logger.info("Loading RDF from entry <${entry.id}>")
            Collection<ReprRef> rdfReferences = getRdfReferences(entry)
            for (ReprRef rdfRef : rdfReferences) {
                logger.info("RDF from <${rdfRef.url}>")
                loadData(conn, rdfRef, entryRepoData.getContext())
            }
        }

        conn.close()

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


class EntryRepoData {

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

    URI id
    Date updated
    RepositoryConnection conn

    private Resource storedContext
    private Resource newContext
    private Literal entryIdLiteral
    private Literal entryUpdatedLiteral

    EntryRepoData(URI id, Date updated, RepositoryConnection conn) {
        this.id = id
        this.updated = updated
        this.conn = conn
        def vf = conn.repository.valueFactory
        entryIdLiteral = vf.createLiteral(id.toString(), XMLSchema.ANYURI)
        entryUpdatedLiteral = RDFUtil.createDateTime(vf, updated)
    }

    Resource getStoredContext() {
        if (storedContext == null) {
            def contextStmt = RDFUtil.one(conn, null, AWOL_ID, entryIdLiteral)
            if (contextStmt != null) {
                storedContext = contextStmt.subject
            }
        }
        return storedContext
    }

    boolean isCollected() {
        getStoredContext()
        if (storedContext != null) {
            def storedUpdated = null
            def updatedStmt = RDFUtil.one(
                    conn, storedContext, AWOL_UPDATED, null)
            if (updatedStmt != null) {
                storedUpdated = updatedStmt.object
            }
            return (entryUpdatedLiteral.equals(storedUpdated))
        }
        return false
    }

    Resource getContext() {
        if (newContext == null) {
            // TODO: use getSelfLink URI (stable)? Cannot for tombstones. Mint?
            def vf = conn.repository.valueFactory
            newContext = vf.createBNode()
        }
        return newContext
    }

    void addContext() {
        // TODO: add in which context (itself - i.e. context)?
        // *Must be removed/updated when the context is*!
        def ctx = getContext()
        conn.add(ctx, RDF.TYPE, AWOL_ENTRY, ctx)
        conn.add(ctx, AWOL_ID, entryIdLiteral, ctx)
        conn.add(ctx, AWOL_UPDATED, entryUpdatedLiteral, ctx)
    }

    void clearContext() {
        conn.clear(getStoredContext())
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
