package se.lagrummet.rinfo.service

import org.openrdf.model.Literal
import org.openrdf.model.Resource
import org.openrdf.model.Value
import org.openrdf.model.ValueFactory
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.XMLSchema
import org.openrdf.repository.RepositoryConnection

import org.openrdf.query.BindingSet
import org.openrdf.query.QueryLanguage
import org.openrdf.query.TupleQuery
import org.openrdf.query.TupleQueryResult

//import static org.apache.commons.codec.digest.DigestUtils.md5Hex


class EntryStats {

    static final SCOVO_ITEM
    static final SCOVO_DIMENSION
    static final TL_AT_YEAR
    static final EVENT_PRODUCT

    static {
        def vf = ValueFactoryImpl.getInstance()

        def SCOVO_NS = "http://purl.org/NET/scovo#"
        def TL_NS = "http://purl.org/NET/c4dm/timeline.owl#"
        def EVENT_NS = "http://purl.org/NET/c4dm/event.owl#"

        SCOVO_ITEM = vf.createURI(SCOVO_NS, "Item")
        SCOVO_DIMENSION = vf.createURI(SCOVO_NS, "dimension")
        TL_AT_YEAR = vf.createURI(TL_NS, "atYear")
        EVENT_PRODUCT = vf.createURI(EVENT_NS, "product")
    }

    // TODO: tag:lagrummet.se,2010:stats#context
    private def timeStatsCtx = [] as Resource[]

    //["dct:contributor", "dct:creator", "dct:publisher", "dct:rightsHolder"]

    RepositoryConnection conn
    Resource entryContext
    org.openrdf.model.URI entryUri
    ValueFactory vf

    EntryStats(repoEntry/*resourceDesc, statsContext*/) {
        this.conn = repoEntry.conn
        this.entryContext = repoEntry.getContext()
        this.entryUri = repoEntry.entryUri
        this.vf = repoEntry.vf
    }

    void addStatistics() {
        def stmts = conn.getStatements(entryUri, null, null, false, entryContext)
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

    void removeStatistics() {
        def eventItemStmt = RDFUtil.one(conn, null, EVENT_PRODUCT, entryUri)
        def eventItem = eventItemStmt ? eventItemStmt.getSubject() : null
        if (!eventItem)
            return
        conn.remove(eventItemStmt, timeStatsCtx)
        // NOTE: if no other products, remove all about eventItem
        if (!conn.hasStatement(eventItem, EVENT_PRODUCT, null, false, timeStatsCtx)) {
            conn.remove(eventItem, null, null, timeStatsCtx)
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
            conn.add(eventItem, RDF.TYPE, SCOVO_ITEM, timeStatsCtx)
            conn.add(eventItem, TL_AT_YEAR, year, timeStatsCtx)
            conn.add(eventItem, SCOVO_DIMENSION, property, timeStatsCtx)
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

}
