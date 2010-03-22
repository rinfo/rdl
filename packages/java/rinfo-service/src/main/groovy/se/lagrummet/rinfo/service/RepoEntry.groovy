package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory

import org.openrdf.model.Literal
import org.openrdf.model.Namespace
import org.openrdf.model.Resource
import org.openrdf.model.Value
import org.openrdf.model.ValueFactory
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.XMLSchema
import org.openrdf.repository.RepositoryConnection
import org.openrdf.rio.RDFFormat
import org.openrdf.query.BindingSet
import org.openrdf.query.QueryLanguage
import org.openrdf.query.TupleQuery
import org.openrdf.query.TupleQueryResult

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
    static final SCOVO_ITEM
    static final SCOVO_DIMENSION
    static final TL_AT_YEAR
    static final EVENT_PRODUCT

    static {
        ValueFactory vf = ValueFactoryImpl.getInstance()
        def AWOL_NS = "http://bblfish.net/work/atom-owl/2006-06-06/#"
        def IANA_NS = "http://www.iana.org/assignments/relation/"
        def FOAF_NS = "http://xmlns.com/foaf/0.1/"
        def SCOVO_NS = "http://purl.org/NET/scovo#"
        def TL_NS = "http://purl.org/NET/c4dm/timeline.owl#"
        def EVENT_NS = "http://purl.org/NET/c4dm/event.owl#"
        AWOL_ENTRY = vf.createURI(AWOL_NS, "Entry")
        AWOL_ID = vf.createURI(AWOL_NS, "id")
        AWOL_UPDATED = vf.createURI(AWOL_NS, "updated")
        AWOL_CONTENT = vf.createURI(AWOL_NS, "content")
        AWOL_TYPE = vf.createURI(AWOL_NS, "type")
        IANA_ALTERNATE = vf.createURI(IANA_NS, "alternate")
        IANA_ENCLOSURE = vf.createURI(IANA_NS, "enclosure")
        FOAF_PRIMARY_TOPIC = vf.createURI(FOAF_NS, "primaryTopic")
        SCOVO_ITEM = vf.createURI(SCOVO_NS, "Item")
        SCOVO_DIMENSION = vf.createURI(SCOVO_NS, "dimension")
        TL_AT_YEAR = vf.createURI(TL_NS, "atYear")
        EVENT_PRODUCT = vf.createURI(EVENT_NS, "product")
    }

    // TODO: try to parse RDFa as well
    def loadableRdfMimeTypes = ["application/rdf+xml"]

    SesameLoader loader
    RepositoryConnection conn
    ValueFactory vf

    URI id
    Date updated
    org.openrdf.model.URI entryUri
    Literal entryIdLiteral
    Literal entryUpdatedLiteral

    private Entry entry

    private Resource storedContext
    private Resource newContext

    private def timeStatsCtx = [] as Resource[] // TODO

    private final logger = LoggerFactory.getLogger(RepoEntry)

    RepoEntry(SesameLoader loader, Entry entry) {
        this(loader, entry.getId().toURI(), entry.getUpdated())
        this.entry = entry
    }

    RepoEntry(SesameLoader loader, URI id, Date updated) {
        this.loader = loader
        this.conn = loader.conn
        this.id = id
        this.updated = updated
        this.vf = conn.getRepository().getValueFactory()
        def idStr = id.toString()
        this.entryUri = vf.createURI(idStr)
        this.entryIdLiteral = vf.createLiteral(idStr, XMLSchema.ANYURI)
        this.entryUpdatedLiteral = RDFUtil.createDateTime(vf, updated)
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
            // TODO:? Ok to mint uri like this?
            newContext = vf.createURI(id.toString() + "/entry#context")
        }
        return newContext
    }

    void create() {
        if (getStoredContext() != null) {
            // NOTE: clear, not remove, since we want to remember seen
            // updates *and* deletes
            // TODO: do we? Only if we want an analogue to the main timeline..?
            clearContext()
        }
        addContext()
        processContents()
        addYearDimension()
    }

    void delete() {
        // TODO: error if already deleted, or doesn't exist at all?
        removeYearDimension()
        clearContext()
        // TODO:? ok to just add the tombstone as a marker (for collect) like this?
        addContext()
    }

    protected void addContext() {
        def ctx = getContext()
        conn.add(ctx, RDF.TYPE, AWOL_ENTRY, ctx)
        conn.add(ctx, AWOL_ID, entryIdLiteral, ctx)
        conn.add(ctx, AWOL_UPDATED, entryUpdatedLiteral, ctx)
        conn.add(ctx, FOAF_PRIMARY_TOPIC, entryUri, ctx)
    }

    protected void clearContext() {
        conn.clear(getStoredContext())
    }

    protected void processContents() {
        def contentElem = entry.contentElement
        def contentUrl = loader.unescapeColon(contentElem.resolvedSrc.toString())
        def contentMediaType = contentElem.mimeType.toString()

        loadData(contentUrl, contentMediaType)
        addAtomEntryMetadata(contentUrl, AWOL_CONTENT, contentMediaType)

        for (link in entry.links) {
            def urlPath = loader.unescapeColon(link.resolvedHref.toString())
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
            addAtomEntryMetadata(urlPath, linkRel, mediaType)
        }
    }

    protected void loadData(String url, String mediaType) {
        if (!loadableRdfMimeTypes.contains(mediaType))
            return
        def ctx = getContext()
        logger.info("Loading RDF from <${url}>")
        def inStream = loader.getResponseAsInputStream(url)
        conn.add(inStream, url, RDFFormat.forMIMEType(mediaType), ctx)
        conn.commit()
    }

    void addAtomEntryMetadata(String url, org.openrdf.model.URI rel, String mediaType) {
        def ctx = getContext()
        def resource = vf.createURI(url)
        conn.add(ctx, rel, resource, ctx)
        conn.add(resource, AWOL_TYPE, vf.createLiteral(mediaType), ctx)
    }

    protected void addYearDimension() {
        def ctx = getContext()
        def stmts = conn.getStatements(entryUri, null, null, false, ctx)
        try {
            while (stmts.hasNext()) {
                def stmt = stmts.next()
                if (!isDateOrDateTime(stmt.object))
                    continue
                Literal year = gYearFromDateTime((Literal)stmt.object)
                Resource eventItem = findOrMakeYearItemFor(stmt.predicate, year)
                conn.add(eventItem, EVENT_PRODUCT, entryUri, timeStatsCtx)
            }
        } finally {
            stmts.close()
        }
    }

    protected boolean isDateOrDateTime(Value value) {
        if (!(value instanceof Literal))
            return false
        return XMLSchema.DATETIME.equals(((Literal) value).getDatatype()) ||
                XMLSchema.DATE.equals(((Literal) value).getDatatype())
    }

    protected Literal gYearFromDateTime(Literal dateTime) {
        String yearRepr = dateTime.calendarValue().getEonAndYear().toString()
        Literal year = vf.createLiteral(yearRepr, XMLSchema.GYEAR)
    }

    protected Resource findOrMakeYearItemFor(property, year) {
        def eventItem = findYearItemFor(property, year)
        if (eventItem == null) {
            eventItem = vf.createBNode()
            conn.add(eventItem, RDF.TYPE, SCOVO_ITEM)
            conn.add(eventItem, TL_AT_YEAR, year)
            conn.add(eventItem, SCOVO_DIMENSION, property)
        }
        return eventItem
    }

    protected Resource findYearItemFor(property, year) {
        Resource eventItem = null
        def yearItemQueryStr = ("SELECT ?eventItem WHERE { " +
                "?eventItem " +
                "    <http://purl.org/NET/c4dm/timeline.owl#atYear> ?year; " +
                "    <http://purl.org/NET/scovo#dimension> ?property . " +
                "} ")
        TupleQuery yearItemQuery = conn.prepareTupleQuery(
                QueryLanguage.SPARQL, yearItemQueryStr)
        yearItemQuery.setBinding("property", property)
        yearItemQuery.setBinding("year", year)
        TupleQueryResult result = yearItemQuery.evaluate()
        try {
            while (result.hasNext()) {
                BindingSet row = result.next()
                eventItem = row.getValue("eventItem")
                break
            }
        } finally {
            result.close()
        }
        return eventItem
    }

    protected void removeYearDimension() {
        def eventItem = findYearItemFor(property, year)
        if (eventItem == null)
            return
        conn.remove(eventItem, EVENT_PRODUCT, entryUri, timeStatsCtx)
    }

}
