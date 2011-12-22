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

    private String collectedLogsBaseUri
    private String entryDatasetUri
    private RepositoryConnection conn

    // TODO:IMPROVE: wrap in CurrentState..
    private Description collectDesc
    Describer currentDescriber
    private String currentFeedUri


    CollectorLogSession(CollectorLog collectorLog, RepositoryConnection conn) {
        this.collectedLogsBaseUri = collectorLog.getSystemBaseUri() + "log/collect"
        this.entryDatasetUri = collectorLog.getEntryDatasetUri()
        this.conn = conn
    }

    void close() {
        conn.close()
    }

    void logFeedPageVisit(URL pageUrl, Feed feed) {
        def feedId = feed.id.toString()
        if (collectDesc == null) {
            initializeCollect(feedId)
        }

        def feedUpdated = feed.getUpdated()
        def collectedFeedUri = createCollectedFeedUri(feedId, feedUpdated, pageUrl.toString())
        currentDescriber = newDescriber(collectedFeedUri)
        def feedDesc = currentDescriber.newDescription(collectedFeedUri, "awol:Feed")

        collectDesc.addRel("iana:via", feedDesc.about)

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
        currentFeedUri = feedDesc.about

        updateCollectInfo()
    }

    void logFeedPageError(Exception e, URL pageUrl) {
        def tstamp = new Date()
        // TODO: we need a source dataset to use...
        def collectedFeedUri = createCollectedFeedUri("", tstamp, pageUrl.toString())
        if (currentDescriber == null) {
            currentDescriber = newDescriber(collectedFeedUri)
        }
        def pageErrorDesc = currentDescriber.newDescription(null, "rc:PageError")
        if (e.message) {
            pageErrorDesc.addLiteral("rdf:value", e.message)
        }
        pageErrorDesc.addRel("dct:source", pageUrl.toString())
        pageErrorDesc.addLiteral("tl:at", dateTime(tstamp))
        //collectDesc.addRel("iana:via", pageErrorDesc.about)
    }

    void initializeCollect(String feedId) {
        // TODO:? resdesign to save collect and *each page* results as RDF to file
        // - requires init to make a dir for current collect

        def desc = newDescriber(false)
        // wipe old (collectContextUri AND related feedPageCtxts)..
        def deletedOldCollect = false
        for (feedAbout in desc.subjectUrisByLiteral("awol:id", feedId)) {
            if (!deletedOldCollect) {
                for (oldCollectUri in desc.subjectUris("iana:via", feedAbout)) {
                    conn.clear(desc.toRef(oldCollectUri))
                    deletedOldCollect = true
                    break
                }
            }
            conn.clear(desc.toRef(feedAbout))
        }
        def startTime = new Date()
        def collectUri = createCollectUri(feedId, startTime)
        collectDesc = newDescriber(collectUri).newDescription(collectUri, "rc:Collect")
        //collectDesc.addLiteral("rdfs:label", "Collect of <"+feedId+">")
        collectDesc.addLiteral("tl:start", dateTime(startTime))
    }

    void updateCollectInfo() {
        collectDesc.remove("tl:end")
        collectDesc.addLiteral("tl:end", dateTime(new Date()))
    }


    void logUpdatedEntry(Feed sourceFeed, Entry sourceEntry, DepotEntry depotEntry) {
        def sourceEntryDesc = makeSourceEntryDesc(sourceEntry)
        def entryDesc = currentDescriber.newDescription(null, "awol:Entry")
        entryDesc.addLiteral("awol:published", dateTime(depotEntry.getPublished()))
        entryDesc.addLiteral("awol:updated", dateTime(depotEntry.getUpdated()))
        entryDesc.addRel("rx:primarySubject", sourceEntry.getId().toString())
        entryDesc.addRel("dct:isPartOf", entryDatasetUri)
        entryDesc.addRel("iana:via", sourceEntryDesc.about)
        updateCollectInfo()
    }

    void logDeletedEntry(Feed sourceFeed, URI sourceEntryId, Date sourceEntryDeleted,
            DepotEntry depotEntry) {
        def deleted = currentDescriber.newDescription(null, "ov:DeletedEntry")
        // TODO: record sourceEntryDeleted:
        //def sourceDeletedEntryDesc = makeSourceDeletedEntryDesc(sourceEntryDeleted)
        //deleted.addRel("iana:via", sourceDeletedEntryDesc.about)
        deleted.addRel("rx:primarySubject", sourceEntryId.toString())
        deleted.addLiteral("tl:at", dateTime(depotEntry.getUpdated()))
        deleted.addRel("dct:isPartOf", entryDatasetUri)
        deleted.addRel("iana:via", currentFeedUri)
        updateCollectInfo()
    }

    ErrorLevel logError(Exception error, Date timestamp, Feed sourceFeed, Entry sourceEntry) {
        Description errorDesc = null
        def errorLevel = ErrorLevel.EXCEPTION
        if (error instanceof SourceCheckException) {
            // TODO: we should change current check procedure per source
            // to *collector*, (from current use in SourceContent)
            //def documentInfo = "type=${src.mediaType};lang=${src.lang};slug=${src.enclosedUriPath}"
            if (error.failedCheck == SourceContent.Check.MD5) {
                errorDesc = currentDescriber.newDescription(null, "rc:ChecksumError")
                def src = (RemoteSourceContent) error.sourceContent
                errorDesc.addLiteral("rc:document", src.urlPath, "xsd:anyURI")
                errorDesc.addLiteral("rc:givenMd5", error.givenValue)
                errorDesc.addLiteral("rc:computedMd5", error.realValue)
            } else if (error.failedCheck == SourceContent.Check.LENGTH) {
                errorDesc = currentDescriber.newDescription(null, "rc:LengthError")
                def src = (RemoteSourceContent) error.sourceContent
                errorDesc.addLiteral("rc:document", src.urlPath, "xsd:anyURI")
                errorDesc.addLiteral("rc:givenLength", error.givenValue)
                errorDesc.addLiteral("rc:computedLength", error.realValue)
            }
            errorLevel = ErrorLevel.ERROR
        } else if (error instanceof IdentifyerMismatchException) {
            errorDesc = currentDescriber.newDescription(null, "rc:IdentifyerError")
            errorDesc.addLiteral("rc:givenUri", error.givenUri)
            errorDesc.addLiteral("rc:computedUri", error.computedUri)
            errorLevel = ErrorLevel.ERROR
        } else if (error instanceof SchemaReportException) {
            def report = error.report
            errorLevel = report.hasErrors? ErrorLevel.ERROR : ErrorLevel.WARNING
            errorDesc = currentDescriber.newDescription(null, "rc:DescriptionError")
            def errorConn = report.connection
            def errorDescriber = newDescriber(errorConn, false)
            ["sch:Error", "sch:Warning"].each {
                errorDescriber.getByType(it).each {
                    errorDesc.addRel("rc:reports", it.about)
                }
            }
            currentDescriber.addFromConnection(errorConn, true)
            report.close()
        }
        if (errorDesc == null) {
            errorDesc = currentDescriber.newDescription(null, "rc:Error")
            errorDesc.addLiteral("rdf:value", error.getMessage() ?: "[empty error message]")
        }
        errorDesc.addLiteral("tl:at", dateTime(new Date()))
        def sourceEntryDesc = makeSourceEntryDesc(sourceEntry)
        errorDesc.addRel("iana:via", sourceEntryDesc.about)
        return errorLevel
    }

    private Description makeSourceEntryDesc(sourceEntry) {
        def sourceEntryDesc = currentDescriber.newDescription(null, "awol:Entry")
        sourceEntryDesc.addLiteral("awol:id", sourceEntry.getId().toURI())
        sourceEntryDesc.addLiteral("awol:updated", dateTime(sourceEntry.getUpdated()))
        sourceEntryDesc.addRel("awol:source", currentFeedUri)
        return sourceEntryDesc
    }


    String createCollectUri(String feedId, Date date) {
        def timestamp = RDFLiteral.parseValue(dateTime(date)).toString()
        def token = md5Hex(feedId + '@' + timestamp)
        return "${collectedLogsBaseUri}/${token}"
    }

    String createCollectedFeedUri(String feedId, Date updated, String url) {
        def timestamp = RDFLiteral.parseValue(dateTime(updated)).toString()
        def token = md5Hex(feedId + '@'+timestamp + '/' + url)
        return "${collectedLogsBaseUri}/feed/${token}"
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
            setPrefix("prv", "http://purl.org/net/provenance/ns#").
            setPrefix("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#").
            setPrefix("iana", "http://www.iana.org/assignments/relation/").
            setPrefix("tl", "http://purl.org/NET/c4dm/timeline.owl#").
            setPrefix("sch", "http://purl.org/net/schemarama#").
            setPrefix("rx", "http://www.w3.org/2008/09/rx#").
            setPrefix("ax", "http://buzzword.org.uk/rdf/atomix#").
            setPrefix("ov", "http://open.vocab.org/terms/").
            setPrefix("rc", "http://rinfo.lagrummet.se/ns/2008/10/collector#")
    }

}
