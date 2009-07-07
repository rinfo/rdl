package se.lagrummet.rinfo.main.storage

import java.net.URISyntaxException

import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed

import org.openrdf.model.ValueFactory
import org.openrdf.model.URI
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.DepotEntry


class FeedCollectorRegistry {

    static final COLLECTOR_NS = "http://rinfo.lagrummet.se/ns/2008/10/collector#"
    static final URI DELETED
    static final URI FROM_FEED
    static final URI LAST_COLLECTED
    static final URI LAST_FEED_ARCHIVE_PAGE
    static final URI STORED_AS
    static final URI UPDATED
    static {
        ValueFactory vf = ValueFactoryImpl.getInstance()
        DELETED = vf.createURI(COLLECTOR_NS, "deleted")
        FROM_FEED = vf.createURI(COLLECTOR_NS, "fromFeed")
        LAST_COLLECTED = vf.createURI(COLLECTOR_NS, "lastCollected")
        LAST_FEED_ARCHIVE_PAGE = vf.createURI(COLLECTOR_NS, "lastFeedArchivePage")
        STORED_AS = vf.createURI(COLLECTOR_NS, "storedAs")
        UPDATED = vf.createURI(COLLECTOR_NS, "updated")
    }

    Repository repo
    private RepositoryConnection conn
    private ValueFactory vf

    FeedCollectorRegistry(Repository repo) {
        this.repo = repo
        this.conn = repo.getConnection()
        this.vf = repo.getValueFactory()
    }

    void shutdown() {
        conn.close()
    }

    void logVisitedFeedPage(URL pageUrl, Feed feed) {
        // TODO: log what?
        def url = feed.getSelfLinkResolvedHref() ?: pageUrl
        def selfUri = vf.createURI(url.toString())
        def updated = RDFUtil.createDateTime(vf, feed.getUpdated())
        conn.remove(selfUri, UPDATED, null)
        conn.add(selfUri, UPDATED, updated)
        // TODO: log isArchive, source, ETag/Modified-Since ...?
    }

    void logUpdatedEntry(Feed sourceFeed, Entry sourceEntry,
            DepotEntry depotEntry) {
        logUpdatedEntry(sourceFeed,
                sourceEntry.getId().toURI(),
                sourceEntry.getUpdated(),
                depotEntry)
    }

    void logUpdatedEntry(Feed sourceFeed,
            java.net.URI sourceEntryId,
            Date updated,
            DepotEntry depotEntry) {
        def sourceUri = vf.createURI(sourceEntryId.toString())
        conn.add(sourceUri, STORED_AS, vf.createURI(
                depotEntry.getId().toString()))
        conn.add(sourceUri, UPDATED, RDFUtil.createDateTime(vf, updated))
        // TODO: how to fail on missing id for source feed?
        //conn.add(sourceUri, FROM_FEED, vf.createURI(sourceFeed.getId().toString()))
    }

    void logDeletedEntry(Feed sourceFeed,
            java.net.URI sourceEntryId,
            Date sourceEntryDeleted,
            DepotEntry depotEntry) {
        logUpdatedEntry(sourceFeed, sourceEntryId, sourceEntryDeleted, depotEntry)
        // TODO: as logUpdatedEntry but add deleted marker? At all?
        //conn.add(sourceUri, DELETED, RDFUtil.createDateTime(vf, sourceEntryDeleted))
        // TODO: see FROM_FEED in logUpdatedEntry
    }

    void logError(Exception error, Date timestamp,
            Feed sourceFeed, Entry sourceEntry) {
        // TODO: spec and implement..
    }

    boolean hasCollected(Entry sourceEntry) {
        def sourceUri = vf.createURI(sourceEntry.getId().toString())
        def sourceUpdated = RDFUtil.createDateTime(vf, sourceEntry.getUpdated())
        def stmt = RDFUtil.one(conn, sourceUri, UPDATED, sourceUpdated)
        // TODO: check for deleted; or better, flag DELETED with bool instead?
        return stmt != null
    }

    /* FIXME: not necessary now when sources use official URI for entry-id:s,
     * remove entire STORED_AS usage.
    java.net.URI getDepotIdBySourceId(java.net.URI sourceEntryId) {
        def sourceUri = vf.createURI(sourceEntryId.toString())
        def stmt = RDFUtil.one(conn, sourceUri, STORED_AS, null)
        if (stmt == null) return null
        return new java.net.URI(stmt.getObject().toString())
    }
    */

}
