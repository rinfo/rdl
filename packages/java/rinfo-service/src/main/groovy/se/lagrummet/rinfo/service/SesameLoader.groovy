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

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.collector.atom.FeedArchivePastToPresentReader
import static se.lagrummet.rinfo.collector.atom.FeedArchiveReader.unescapeColon
import se.lagrummet.rinfo.collector.atom.AtomEntryDeleteUtil


class SesameLoader extends FeedArchivePastToPresentReader {

    Repository repository
    RepositoryConnection conn

    private final logger = LoggerFactory.getLogger(SesameLoader)


    SesameLoader(Repository repository) {
        this.repository = repository
    }

    @Override
    public void initialize() {
        super.initialize()
        conn = repository.getConnection()
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown()
        } finally {
            conn.close()
        }
    }

    boolean stopOnEntry(Entry entry) {
        // TODO:? store "youngest collected entry + date", stop only on that?
        // (pageUrl, entry and date)
        def repoEntry = new RepoEntry(
                entry.id.toURI(), entry.updated, conn)
        return repoEntry.isCollected()
    }

    void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deletedMap) {

        deleteFromMarkers(feed, deletedMap)

        for (Entry entry : effectiveEntries) {
            def repoEntry = new RepoEntry(entry.id.toURI(), entry.updated, conn)
            // TODO: isn't this a strange exceptional state now?
            // (FeedArchivePastToPresentReader shouldn't supply known stuff..)
            if (repoEntry.isCollected()) {
                logger.debug("skipping collected entry <${entry.id}> [${entry.updated}]")
                continue
            } else {
                if (repoEntry.getStoredContext() != null) {
                    // NOTE: clear, not remove, since we want to remember seen
                    // updates *and* deletes
                    repoEntry.clearContext()
                }
            }
            repoEntry.addContext()
            repoEntry.addAtomEntryMetadata(entry)
            logger.info("Loading RDF from entry <${entry.id}>")
            Collection<ReprRef> rdfReferences = getRdfReferences(entry)
            for (ReprRef rdfRef : rdfReferences) {
                logger.info("RDF from <${rdfRef.url}>")
                loadData(rdfRef, repoEntry.getContext())
            }
            // TODO:? rdataService.addContextEntryAnnotations(...)
        }
    }

    protected void deleteFromMarkers(Feed sourceFeed, Map<IRI, AtomDate> deletedMap) {
        for (Map.Entry<URI, Date> delItem : deletedMap.entrySet()) {
            def repoEntry = new RepoEntry(
                    delItem.getKey().toURI(), delItem.getValue().getDate(),
                    conn)
            // TODO: unless already deleted..
            logger.info("Deleting RDF from entry <${repoEntry.id}>")
            repoEntry.clearContext() // TODO: error if not exists?
            // TODO:? ok to just add the tombstone as a marker (for collect) like this?
            repoEntry.addContext()
        }
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
            def mediaType = link.getMimeType().toString()
            if (rdfMimeType.equals(mediaType) &&
                ("alternate".equals(link.rel) || "enclosure".equals(link.rel)) ) {
                rdfReferences.add(new ReprRef(new URL(urlPath), mediaType))
            }
        }
        return rdfReferences
    }

    protected void loadData(ReprRef repr, Resource context) {
        def inStream = getResponseAsInputStream(repr.url)
        conn.add(
                inStream, repr.url.toString(), RDFFormat.forMIMEType(repr.mediaType),
                context)
        conn.commit()
    }

}


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
        ValueFactory vf = ValueFactoryImpl.getInstance()
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

    URI id
    Date updated
    RepositoryConnection conn
    ValueFactory vf

    private Resource storedContext
    private Resource newContext
    private Literal entryIdLiteral
    private Literal entryUpdatedLiteral

    RepoEntry(URI id, Date updated, RepositoryConnection conn) {
        this.id = id
        this.updated = updated
        this.conn = conn
        vf = conn.repository.valueFactory
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

    Resource getContext() {
        if (newContext == null) {
            // TODO:? Mint uri from id? Cannot use getSelfLink for tombstones.
            newContext = vf.createURI(id.toString() + "/entry#context")
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
        conn.add(ctx, FOAF_PRIMARY_TOPIC, vf.createURI(id.toString()), ctx)
    }

    void addAtomEntryMetadata(Entry entry) {
        def ctx = getContext()
        def contentElem = entry.contentElement
        def contentUrlPath = unescapeColon(contentElem.resolvedSrc.toString())
        def content = vf.createURI(contentUrlPath)
        conn.add(ctx, AWOL_CONTENT, content, ctx)
        def contentMimeType = vf.createLiteral(contentElem.mimeType.toString())
        conn.add(content, AWOL_TYPE, contentMimeType, ctx)
        for (link in entry.links) {
            def linkRel = null
            if (link.rel == "alternate") {
                linkRel = IANA_ALTERNATE
            } else if (link.rel == "enclosure") {
                linkRel = IANA_ENCLOSURE
            } else {
                //linkRel = vf.createURI(link.rel, base=IANA_NS)
            }
            if (linkRel == null)
                continue
            def urlPath = unescapeColon(link.resolvedHref.toString())
            def linkUri = vf.createURI(urlPath)
            conn.add(ctx, linkRel, linkUri, ctx)
            def mediaType = vf.createLiteral(link.getMimeType().toString())
            conn.add(linkUri, AWOL_TYPE, mediaType, ctx)
        }
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
