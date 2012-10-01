package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory

import org.openrdf.model.Literal
import org.openrdf.model.Namespace
import org.openrdf.model.Resource
import org.openrdf.model.ValueFactory
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.XMLSchema
import org.openrdf.repository.RepositoryConnection

import org.apache.abdera.model.Entry

import se.lagrummet.rinfo.base.rdf.RDFUtil


class RepoEntry {

    static final AWOL_ENTRY
    static final AWOL_ID
    static final AWOL_UPDATED
    static final AWOL_CONTENT
    static final AWOL_TYPE
    static final IANA_ALTERNATE
    static final IANA_ENCLOSURE
    static final FOAF_PRIMARY_TOPIC

    static {
        def vf = ValueFactoryImpl.getInstance()

        def AWOL_NS = "http://bblfish.net/work/atom-owl/2006-06-06/#"
        def IANA_NS = "http://www.iana.org/assignments/relation/"
        def FOAF_NS = "http://xmlns.com/foaf/0.1/"

        AWOL_ENTRY = vf.createURI(AWOL_NS, "Entry")
        AWOL_ID = vf.createURI(AWOL_NS, "id")
        AWOL_UPDATED = vf.createURI(AWOL_NS, "updated")
        AWOL_CONTENT = vf.createURI(AWOL_NS, "content")
        AWOL_TYPE = vf.createURI(AWOL_NS, "type")
        IANA_ALTERNATE = vf.createURI(IANA_NS, "alternate")
        IANA_ENCLOSURE = vf.createURI(IANA_NS, "enclosure")
        FOAF_PRIMARY_TOPIC = vf.createURI(FOAF_NS, "primaryTopic")
    }

    static final CONTEXT_SUFFIX = "/entry#context"

    def loadableRdfMimeTypes = [
        "application/rdf+xml",
        "application/xhtml+xml"
    ]

    SesameLoader loader
    RepositoryConnection conn
    ValueFactory vf

    URI id
    Date updated
    org.openrdf.model.URI entryUri
    Literal entryIdLiteral
    Literal entryUpdatedLiteral
    EntryStats entryStats

    private Entry entry

    private Resource storedContext
    private Resource newContext

    private final logger = LoggerFactory.getLogger(RepoEntry)

    RepoEntry(SesameLoader loader, Entry entry) {
        this(loader, entry.getId().toURI(), entry.getUpdated())
        this.entry = entry
    }

    RepoEntry(SesameLoader loader, URI id, Date updated) {
        this.loader = loader
        this.conn = loader.conn
        conn.setAutoCommit(false)
        this.id = id
        this.updated = updated
        this.vf = conn.getRepository().getValueFactory()
        def idStr = id.toString()
        this.entryUri = vf.createURI(idStr)
        this.entryIdLiteral = vf.createLiteral(idStr, XMLSchema.ANYURI)
        this.entryUpdatedLiteral = RDFUtil.createDateTime(vf, updated)
        this.entryStats = new EntryStats(conn, idStr, getContext().stringValue())
    }

    Resource getContext() {
        if (newContext == null) {
            // TODO:? Ok to mint uri like this?
            newContext = vf.createURI(id.toString() + CONTEXT_SUFFIX)
        }
        return newContext
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
        if (getStoredContext() != null) {
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

    void create() {
        try {
            if (getStoredContext() != null) {
                // NOTE: clear, not remove, since we want to remember seen
                // updates *and* deletes
                // TODO: do we? Only if we want an analogue to the main timeline..?
                clearContext()
            }
            addContext()
            processContents()
            conn.commit() // TODO: either load into mem first, or do something
                          // if addStatistics fails...
            entryStats.addStatistics()
        } catch (Exception e) {
            conn.rollback()
            throw e
        }
        conn.commit()
    }

    void delete() {
        // TODO: throw error if already deleted or missing?
        try {
            entryStats.removeStatistics()
            clearContext()
            // TODO:? add the tombstone as a marker (e.g. for isCollected during collect)?
            //addTombstoneContext()
        } catch (Exception e) {
            conn.rollback()
            throw e
        }
        conn.commit()
    }

    protected void addContext() {
        def ctx = getContext()
        conn.add(ctx, RDF.TYPE, AWOL_ENTRY, ctx)
        conn.add(ctx, AWOL_ID, entryIdLiteral, ctx)
        conn.add(ctx, AWOL_UPDATED, entryUpdatedLiteral, ctx)
        conn.add(ctx, FOAF_PRIMARY_TOPIC, entryUri, ctx)
    }

    protected void clearContext() {
        def ctx = getStoredContext()
        if (ctx != null)
            conn.clear(ctx)
    }

    protected void processContents() {
        def contentElem = entry.contentElement
        if (contentElem == null || contentElem.resolvedSrc == null)
            return // TODO:? handle embedded if ok mimeType? And/or warn if no data?
        def contentUrl = contentElem.resolvedSrc.toString()
        def contentMediaType = contentElem.mimeType.toString()

        loadData(contentUrl, contentMediaType)
        addAtomEntryLinkMetadata(contentUrl, AWOL_CONTENT, contentMediaType)

        for (link in entry.links) {
            def urlPath = link.resolvedHref.toString()
            def mediaType = link.getMimeType().toString()
            if ("alternate".equals(link.rel) || "enclosure".equals(link.rel)) {
                loadData(urlPath, mediaType)
            }
            def linkRel = null
            if (link.rel == "alternate") {
                linkRel = IANA_ALTERNATE
            } else if (link.rel == "enclosure") {
                linkRel = IANA_ENCLOSURE
            } else {
                continue
                //linkRel = vf.createURI(link.rel, base=IANA_NS)
            }
            addAtomEntryLinkMetadata(urlPath, linkRel, mediaType)
        }
    }

    protected void loadData(String url, String mediaType) {
        if (!loadableRdfMimeTypes.contains(mediaType))
            return
        def ctx = getContext()
        logger.info("Loading RDF from <${url}>")
        def inStream = loader.getResponseAsInputStream(url)
        try {
          RDFUtil.loadDataFromStream(conn, inStream, url, mediaType, ctx)
        } finally {
          inStream.close()
        }
    }

    void addAtomEntryLinkMetadata(String url,
            org.openrdf.model.URI rel, String mediaType) {
        def ctx = getContext()
        def resource = vf.createURI(url)
        conn.add(ctx, rel, resource, ctx)
        conn.add(resource, AWOL_TYPE, vf.createLiteral(mediaType), ctx)
    }

}
