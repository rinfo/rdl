package se.lagrummet.rinfo.main.storage

import static org.apache.commons.codec.digest.DigestUtils.md5Hex

import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.ext.history.FeedPagingHelper

import org.openrdf.repository.RepositoryConnection

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description

import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.SourceContent
import se.lagrummet.rinfo.store.depot.SourceCheckException

import se.lagrummet.rinfo.main.storage.log.*


/**
 * A Session for the CollectorLog gives access to the creation of logged events
 * for collector activity.
 * This is a stateful object that is not thread-safe and must be closed.
 */
class CollectorLogSession {

    Describer describer

    private String systemBaseUri
    private String entryDatasetUri

    private Description collectDesc
    private String currentFeedUri


    CollectorLogSession(CollectorLog collectorLog, RepositoryConnection conn) {
        this.systemBaseUri = collectorLog.getSystemBaseUri()
        this.entryDatasetUri = collectorLog.getEntryDatasetUri()
        this.describer = newDescriber(conn)
    }

    void close() {
        describer.close()
    }


    void logFeedPageVisit(URL pageUrl, Feed feed) {
        if (collectDesc == null) {
            def collectStartTime = new Date()
            collectDesc = describer.newDescription(
                    createCollectUri(collectStartTime), "rc:Collect")
            collectDesc.addValue("tl:start", dateTime(collectStartTime))
        }

        def feedUrl = feed.getSelfLinkResolvedHref() ?: pageUrl
        def feedId = feed.id.toURI()
        def feedUpdated = feed.getUpdated()

        def feedDesc = describer.newDescription(
                createCollectedFeedUri(feedId, feedUpdated, pageUrl), "awol:Feed")

        collectDesc.addRel("iana:via", feedDesc.about)

        feedDesc.addValue("awol:id", feedId)
        feedDesc.addValue("awol:updated", feedUpdated)
        feedDesc.addRel("iana:self", feedUrl.toString())

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

    void logUpdatedEntry(Feed sourceFeed, Entry sourceEntry, DepotEntry depotEntry) {
        def sourceEntryDesc = makeSourceEntryDesc(sourceEntry)
        def entryDesc = describer.newDescription(null, "awol:Entry")
        entryDesc.addValue("awol:published", dateTime(depotEntry.getPublished()))
        entryDesc.addValue("awol:updated", dateTime(depotEntry.getUpdated()))
        entryDesc.addRel("rx:primarySubject", sourceEntry.getId().toString())
        entryDesc.addRel("dct:isPartOf", entryDatasetUri)
        entryDesc.addRel("iana:via", sourceEntryDesc.about)
        updateCollectInfo()
    }

    void logDeletedEntry(Feed sourceFeed, URI sourceEntryId, Date sourceEntryDeleted,
            DepotEntry depotEntry) {
        def deleted = describer.newDescription(null, "ov:DeletedEntry")
        // TODO: record sourceEntryDeleted:
        //def sourceDeletedEntryDesc = makeSourceDeletedEntryDesc(sourceEntryDeleted)
        //deleted.addRel("iana:via", sourceDeletedEntryDesc.about)
        deleted.addRel("rx:primarySubject", sourceEntryId.toString())
        deleted.addValue("tl:at", dateTime(depotEntry.getUpdated()))
        deleted.addRel("dct:isPartOf", entryDatasetUri)
        deleted.addRel("iana:via", currentFeedUri)
        updateCollectInfo()
    }

    void logError(Exception error, Date timestamp, Feed sourceFeed, Entry sourceEntry) {
        Description errorDesc = null
        if (error instanceof SourceCheckException) {
            if (error.failedCheck == SourceContent.Check.MD5) {
                errorDesc = describer.newDescription(null, "rc:ChecksumError")
                // FIXME: now, we need to handle checks per source
                // in *collector*, not in SourceContent
                def src = (RemoteSourceContent) error.sourceContent
                errorDesc.addValue("rc:document", src.urlPath)
                //def documentInfo = "type=${src.mediaType};lang=${src.lang};slug=${src.enclosedUriPath}"
                errorDesc.addValue("rc:givenMd5", error.givenValue)
                errorDesc.addValue("rc:computedMd5", error.realValue)
            }
            // TODO: length
        } else if (error instanceof IdentifyerMismatchException) {
            errorDesc = describer.newDescription(null, "rc:IdentifyerError")
            errorDesc.addValue("rc:givenUri", error.givenUri)
            errorDesc.addValue("rc:computedUri", error.computedUri)
        }
        if (errorDesc == null) {
            errorDesc = describer.newDescription(null, "rc:Error")
            errorDesc.addValue("rdf:value", error.getMessage() ?: "[empty error message]")
        }
        errorDesc.addValue("tl:at", dateTime(new Date()))
        def sourceEntryDesc = makeSourceEntryDesc(sourceEntry)
        errorDesc.addValue("iana:via", sourceEntryDesc)
    }

    private Description makeSourceEntryDesc(sourceEntry) {
        def sourceEntryDesc = describer.newDescription(null, "awol:Entry")
        sourceEntryDesc.addValue("awol:id", sourceEntry.getId().toURI())
        sourceEntryDesc.addValue("awol:updated", dateTime(sourceEntry.getUpdated()))
        // TODO: setSourceRef(createCollectedFeedUri(sourceFeed)) for serializ.
        sourceEntryDesc.addValue("awol:source", currentFeedUri)
        return sourceEntryDesc
    }

    // TODO: (optional) save *each* log method RDF to file
    // - requires init to make a dir for current collect
    void updateCollectInfo() {
        collectDesc.remove("tl:end")
        collectDesc.addValue("tl:end", dateTime(new Date()))
    }


    String createCollectUri(Date date) {
        String w3cDate = describer.toLiteral(dateTime(date)).stringValue()
        return "${systemBaseUri}log/collect/${w3cDate}"
    }

    String createCollectedFeedUri(id, Date updated, url) {
        String w3cDate = describer.toLiteral(dateTime(updated)).stringValue()
        def token = md5Hex(id.toString() + '@'+w3cDate + '/' + url.toString())
        return "${systemBaseUri}log/collect/${token}"
    }

    GregorianCalendar dateTime(Date time) {
        GregorianCalendar grCal = new GregorianCalendar(
                TimeZone.getTimeZone("GMT"));
        grCal.setTime(time);
        return grCal;
    }

    protected Describer newDescriber(conn) {
        def desc = new Describer(conn, true)
        return desc.setPrefix("dct", "http://purl.org/dc/terms/").
            //setPrefix("prv", "http://purl.org/net/provenance/ns#")
            setPrefix("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#").
            setPrefix("iana", "http://www.iana.org/assignments/relation/").
            setPrefix("tl", "http://purl.org/NET/c4dm/timeline.owl#").
            setPrefix("rx", "http://www.w3.org/2008/09/rx#").
            setPrefix("ax", "http://buzzword.org.uk/rdf/atomix#").
            setPrefix("ov", "http://open.vocab.org/terms/").
            setPrefix("rc", "http://rinfo.lagrummet.se/ns/2008/10/collector#")
    }

}
