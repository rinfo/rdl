package se.lagrummet.rinfo.main.storage

import static org.apache.commons.codec.digest.DigestUtils.md5Hex

import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.ext.history.FeedPagingHelper

import org.openrdf.repository.RepositoryConnection

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description
import se.lagrummet.rinfo.base.rdf.RDFLiteral

import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.SourceContent
import se.lagrummet.rinfo.store.depot.SourceCheckException


/**
 * A Session for the CollectorLog gives access to the creation of logged events
 * for collector activity.
 * This is a stateful object that is not thread-safe and must be closed.
 */
class CollectorLogSession implements Closeable {

    private String reportBaseUri
    private String systemDatasetUri
    private RepositoryConnection conn

    private SessionState state

    class SessionState {
        String currentFeedId
        Description collectDesc
        Describer pageDescriber
        String currentFeedUri
    }


    CollectorLogSession(CollectorLog collectorLog, RepositoryConnection conn) {
        this.reportBaseUri = collectorLog.reportBaseUri
        this.systemDatasetUri = collectorLog.systemDatasetUri
        this.conn = conn
    }

    void start(StorageCredentials credentials) {
        if (credentials.source == null) {
            return
        }
        state = initializeCollect(credentials.source)
    }

    void close() {
        state.collectDesc.addLiteral("prov:endedAtTime", dateTime(new Date()))
        conn.close()
    }


    SessionState initializeCollect(CollectorSource source) {
        // TODO:? resdesign to save collect and *each page* results as RDF to file
        // - requires init to make a dir for current collect
        def feedId = source.feedId.toString()
        def describer = newDescriber(false)
        // Wipe old (collectContextUri AND related feedPageCtxts):
        def deletedOldCollect = false
        for (activityDesc in describer.subjects("prov:wasInfluencedBy", feedId)) {
            if (!deletedOldCollect) {
                for (oldCollectUri in activityDesc.objectUris("prov:used")) {
                    conn.clear(describer.toRef(oldCollectUri))
                    deletedOldCollect = true
                    break
                }
            }
            conn.clear(describer.toRef(feedAbout))
        }
        def startTime = new Date()
        def state = new SessionState()
        state.currentFeedId = feedId
        def collectUri = createCollectUri(feedId, startTime)
        state.collectDesc = newDescriber(collectUri).newDescription(collectUri,
                "prov:Activity")
        state.collectDesc.addLiteral("prov:startedAtTime", dateTime(startTime))
        state.collectDesc.addRel("prov:wasInfluencedBy", feedId)
        //state.collectDesc.addRel("prov:wasAssociatedWith", source.agentUri)
        return state
    }

    String createCollectUri(String feedId, Date date) {
        //def timestamp = RDFLiteral.parseValue(dateTime(date)).toString()
        //def token = md5Hex(feedId + '@' + timestamp)
        return reportBaseUri + feedId +"/latest"
    }

    String createCollectedFeedUri(String feedId, Date updated, String url) {
        //def timestamp = RDFLiteral.parseValue(dateTime(updated)).toString()
        //def token = md5Hex(feedId + '@'+timestamp + '/' + url)
        return reportBaseUri + feedId +"/latest" + new URL(url).path
    }

    void logFeedPageVisit(URL pageUrl, Feed feed) {
        assert state.currentFeedId == feedId
        def feedId = feed.id.toString()

        def feedUpdated = feed.getUpdated()
        def collectedFeedUri = createCollectedFeedUri(feedId, feedUpdated, pageUrl.toString())
        state.pageDescriber = newDescriber(collectedFeedUri)
        def feedDesc = state.pageDescriber.newDescription(collectedFeedUri, "awol:Feed")

        state.collectDesc.addRel("prov:used", feedDesc.about)

        feedDesc.addLiteral("awol:id", feedId)
        feedDesc.addLiteral("awol:updated", feedUpdated)
        def feedUrl = (feed.getSelfLinkResolvedHref() ?: pageUrl).toString()
        feedDesc.addRel("iana:self", feedUrl)

        if (FeedPagingHelper.isComplete(feed))
            feedDesc.addType("ax:CompleteFeed")
        if (FeedPagingHelper.isArchive(feed))
            feedDesc.addType("ax:ArchiveFeed")

        if (FeedPagingHelper.getCurrent(feed) != null)
            feedDesc.addRel("iana:current", FeedPagingHelper.getCurrent(feed).toString())
        if (FeedPagingHelper.getPreviousArchive(feed) != null)
            feedDesc.addRel("iana:prev-archive", FeedPagingHelper.getPreviousArchive(feed).toString())
        if (FeedPagingHelper.getNextArchive(feed) != null)
            feedDesc.addRel("iana:next-archive", FeedPagingHelper.getNextArchive(feed).toString())

        // TODO: log HTTP headers for ETag/Modified-Since ...?
        state.currentFeedUri = feedDesc.about
    }

    void logFeedPageError(Exception e, URL pageUrl) {
        def tstamp = new Date()
        if (state.pageDescriber == null) {
            def collectedFeedUri = createCollectedFeedUri(
                    state.currentFeedId, tstamp, pageUrl.toString())
            state.pageDescriber = newDescriber(collectedFeedUri)
        }
        def pageErrorDesc = state.pageDescriber.newDescription(null, "rc:PageError")
        if (e.message) {
            pageErrorDesc.addLiteral("rdf:value", e.message)
        }
        pageErrorDesc.addRel("prov:wasDerivedFrom", pageUrl.toString())
        pageErrorDesc.addLiteral("prov:atTime", dateTime(tstamp))
        state.collectDesc.addRel("prov:generated", pageErrorDesc.about)
    }


    void logUpdatedEntry(Feed sourceFeed, Entry sourceEntry, DepotEntry depotEntry) {
        def sourceEntryDesc = makeSourceEntryDesc(sourceEntry)
        def entryDesc = state.pageDescriber.newDescription(null, "awol:Entry")
        entryDesc.addLiteral("awol:published", dateTime(depotEntry.getPublished()))
        entryDesc.addLiteral("awol:updated", dateTime(depotEntry.getUpdated()))
        entryDesc.addRel("foaf:primaryTopic", sourceEntry.getId().toString())
        entryDesc.addRel("dct:isPartOf", systemDatasetUri)
        entryDesc.addRel("prov:wasQuotedFrom", sourceEntryDesc.about)
    }

    void logDeletedEntry(Feed sourceFeed, URI sourceEntryId, Date sourceEntryDeleted,
            DepotEntry depotEntry) {
        def deleted = state.pageDescriber.newDescription(null, "ov:DeletedEntry")
        // TODO: record sourceEntryDeleted:
        //def sourceDeletedEntryDesc = makeSourceDeletedEntryDesc(sourceEntryDeleted)
        //deleted.addRel("prov:wasQuotedFrom", sourceDeletedEntryDesc.about)
        deleted.addRel("foaf:primaryTopic", sourceEntryId.toString())
        deleted.addLiteral("prov:atTime", dateTime(depotEntry.getUpdated()))
        deleted.addRel("dct:isPartOf", systemDatasetUri)
        deleted.addRel("prov:wasDerivedFrom", state.currentFeedUri)
    }

    ErrorLevel logError(Exception error, Date timestamp, Feed sourceFeed, Entry sourceEntry) {
        Description errorDesc = null
        def errorLevel = ErrorLevel.EXCEPTION
        if (error instanceof SourceCheckException) {
            // TODO: we should change current check procedure per source
            // to *collector*, (from current use in SourceContent)
            //def documentInfo = "type=${src.mediaType};lang=${src.lang};slug=${src.enclosedUriPath}"
            if (error.failedCheck == SourceContent.Check.MD5) {
                errorDesc = state.pageDescriber.newDescription(null, "rc:ChecksumError")
                def src = (RemoteSourceContent) error.sourceContent
                errorDesc.addLiteral("rc:document", src.urlPath, "xsd:anyURI")
                errorDesc.addLiteral("rc:givenMd5", error.givenValue)
                errorDesc.addLiteral("rc:computedMd5", error.realValue)
            } else if (error.failedCheck == SourceContent.Check.LENGTH) {
                errorDesc = state.pageDescriber.newDescription(null, "rc:LengthError")
                def src = (RemoteSourceContent) error.sourceContent
                errorDesc.addLiteral("rc:document", src.urlPath, "xsd:anyURI")
                errorDesc.addLiteral("rc:givenLength", error.givenValue)
                errorDesc.addLiteral("rc:computedLength", error.realValue)
            }
            errorLevel = ErrorLevel.ERROR
        } else if (error instanceof IdentifyerMismatchException) {
            errorDesc = state.pageDescriber.newDescription(null, "rc:IdentifyerError")
            errorDesc.addLiteral("rc:givenUri", error.givenUri)
            errorDesc.addLiteral("rc:computedUri", error.computedUri ?: "")
            errorLevel = ErrorLevel.ERROR
        } else if (error instanceof SchemaReportException) {
            def report = error.report
            errorLevel = report.hasErrors? ErrorLevel.ERROR : ErrorLevel.WARNING
            errorDesc = state.pageDescriber.newDescription(null, "rc:DescriptionError")
            def errorConn = report.connection
            def errorDescriber = newDescriber(errorConn, false)
            ["sch:Error", "sch:Warning"].each {
                errorDescriber.getByType(it).each {
                    errorDesc.addRel("rc:reports", it.about)
                }
            }
            state.pageDescriber.addFromConnection(errorConn, true)
            report.close()
        }
        if (errorDesc == null) {
            errorDesc = state.pageDescriber.newDescription(null, "rc:Error")
            def repr = error.cause?.toString() ?: error.toString() ?: "[N/A]"
            errorDesc.addLiteral("rdf:value", repr)
        }
        errorDesc.addLiteral("prov:atTime", dateTime(new Date()))
        def sourceEntryDesc = makeSourceEntryDesc(sourceEntry)
        errorDesc.addRel("prov:wasDerivedFrom", sourceEntryDesc.about)
        return errorLevel
    }

    private Description makeSourceEntryDesc(sourceEntry) {
        def sourceEntryDesc = state.pageDescriber.newDescription(null, "awol:Entry")
        sourceEntryDesc.addLiteral("awol:id", sourceEntry.getId().toURI())
        sourceEntryDesc.addLiteral("awol:updated", dateTime(sourceEntry.getUpdated()))
        sourceEntryDesc.addRel("awol:source", state.currentFeedUri)
        return sourceEntryDesc
    }


    GregorianCalendar dateTime(Date time) {
        return RDFLiteral.toGCal(time, "GMT");
    }

    protected Describer newDescriber(String... contexts) {
        return newDescriber(true, contexts)
    }

    protected Describer newDescriber(boolean storePrefixes, String... contexts) {
        return newDescriber(conn, true, contexts)
    }

    protected Describer newDescriber(RepositoryConnection conn,
            boolean storePrefixes, String... contexts) {
        def desc = new Describer(conn, storePrefixes, contexts)
        return desc.setPrefix("dct", "http://purl.org/dc/terms/").
            setPrefix("prov", "http://www.w3.org/ns/prov#").
            setPrefix("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#").
            setPrefix("iana", "http://www.iana.org/assignments/relation/").
            setPrefix("sch", "http://purl.org/net/schemarama#").
            setPrefix("foaf", "http://xmlns.com/foaf/0.1/").
            setPrefix("ax", "http://buzzword.org.uk/rdf/atomix#").
            setPrefix("ov", "http://open.vocab.org/terms/").
            setPrefix("rc", "http://rinfo.lagrummet.se/ns/2008/10/collector#")
    }

}
