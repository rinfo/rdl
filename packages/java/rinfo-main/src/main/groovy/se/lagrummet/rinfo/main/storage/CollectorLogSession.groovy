package se.lagrummet.rinfo.main.storage

import java.net.URISyntaxException

import javax.xml.namespace.QName
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.DatatypeConfigurationException
import javax.xml.datatype.XMLGregorianCalendar

import static org.apache.commons.codec.digest.DigestUtils.md5Hex

import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed

import org.openrdf.model.ValueFactory
import org.openrdf.model.URI
import org.openrdf.model.impl.URIImpl
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection

import org.openrdf.elmo.ElmoManager

import se.lagrummet.rinfo.base.rdf.RDFUtil

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

    private ElmoManager manager

    private String systemBaseUri
    private URI entryIsPartOfRdfUri

    private CollectEvent collectEvent
    private FeedEvent currentFeedEvent // TODO: don't keep; use it's uri..


    CollectorLogSession(CollectorLog collectorLog, ElmoManager manager) {
        this.systemBaseUri = collectorLog.getSystemBaseUri()
        this.entryIsPartOfRdfUri = new URIImpl(collectorLog.getEntryDatasetUri())
        this.manager = manager
        def collectStartTime = new Date()
        collectEvent = manager.create(
                createCollectUri(collectStartTime), CollectEvent)
        collectEvent.setStart(createXmlGrCal(collectStartTime))
    }

    public ElmoManager getManager() { return manager; }

    void close() {
        manager.close()
    }

    void logFeedPageVisit(URL pageUrl, Feed sourceFeed) {
        def feedUrl = sourceFeed.getSelfLinkResolvedHref() ?: pageUrl
        def feedId = sourceFeed.id.toURI()
        def feedTimeCal = createXmlGrCal(sourceFeed.getUpdated())
        def feedEvent = manager.create(
                createCollectedFeedUri(feedId, feedTimeCal, pageUrl), FeedEvent)
        feedEvent.setId(feedId)
        feedEvent.setUpdated(feedTimeCal)
        feedEvent.setSelf(new URIImpl(feedUrl.toString()))
        collectEvent.getViaFeeds().add(feedEvent)
        // TODO: log isArchive, source, ETag/Modified-Since ...?
        // TODO: log methods also receive sourceFeed, out rdf should only maintain
        // the same URI ref, not store all feed statements...
        currentFeedEvent = feedEvent
        completeLogEvent()
    }

    void logUpdatedEntry(Feed sourceFeed, Entry sourceEntry,
            DepotEntry depotEntry) {
        def sourceEntryEvent = createSourceEntryEvent(sourceEntry)
        def entryEvent = manager.create(EntryEvent)
        entryEvent.setPublished(createXmlGrCal(depotEntry.getPublished()))
        entryEvent.setUpdated(createXmlGrCal(depotEntry.getUpdated()))
        entryEvent.setPrimarySubject(new URIImpl(sourceEntry.getId().toString()))
        entryEvent.setIsPartOf(entryIsPartOfRdfUri)
        entryEvent.setViaEntry(sourceEntryEvent)
        completeLogEvent()
    }

    void logDeletedEntry(Feed sourceFeed,
            java.net.URI sourceEntryId,
            Date sourceEntryDeleted,
            DepotEntry depotEntry) {
        def deleted = manager.create(DeletedEntryEvent)
        deleted.setPrimarySubject(new URIImpl(sourceEntryId.toString()))
        // TODO: record sourceEntryDeleted in viaDeletedEntryEvent?
        deleted.setAt(createXmlGrCal(depotEntry.getUpdated()))
        deleted.setIsPartOf(entryIsPartOfRdfUri)
        deleted.setViaFeed(currentFeedEvent)
        completeLogEvent()
    }

    void logError(Exception error, Date timestamp,
            Feed sourceFeed, Entry sourceEntry) {
        ErrorEvent errorEvent = null
        if (error instanceof SourceCheckException) {
            if (error.failedCheck == SourceContent.Check.MD5) {
                errorEvent = manager.create(ChecksumErrorEvent)
                // FIXME: now, we need to handle checks per source
                // in *collector*, not in SourceContent
                def src = error.sourceContent
                def documentInfo = "type=${src.mediaType};lang=${src.lang};slug=${src.enclosedUriPath}"
                errorEvent.setDocument(documentInfo)
                errorEvent.setGivenMd5(error.givenValue)
                errorEvent.setComputedMd5(error.realValue)
            }
            // TODO: length
        } else if (error instanceof IdentifyerMismatchException) {
            errorEvent = manager.create(IdentifyerErrorEvent)
            // TODO: get given and computed
            errorEvent.setGivenUri(error.givenUri)
            errorEvent.setComputedUri(error.computedUri)
        }
        if (errorEvent == null) {
            errorEvent = manager.create(ErrorEvent)
            errorEvent.setValue(error.getMessage())
        }
        errorEvent.setAt(createXmlGrCal(new Date()))
        def sourceEntryEvent = createSourceEntryEvent(sourceEntry)
        errorEvent.setViaEntry(sourceEntryEvent)
    }

    private EntryEvent createSourceEntryEvent(sourceEntry) {
        def sourceEntryEvent = manager.create(EntryEvent)
        sourceEntryEvent.setId(sourceEntry.getId().toURI())
        sourceEntryEvent.setUpdated(createXmlGrCal(sourceEntry.getUpdated()))
        // TODO: setSourceRef(createCollectedFeedUri(sourceFeed)) for serializ.
        sourceEntryEvent.setSource(currentFeedEvent)
        return sourceEntryEvent
    }

    // TODO: polish these {{{
    XMLGregorianCalendar createXmlGrCal(Date time) {
        GregorianCalendar grCal = new GregorianCalendar(
                TimeZone.getTimeZone("GMT"));
        grCal.setTime(time);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(grCal);
    }

    QName createCollectUri(date) {
        return new QName(systemBaseUri, "event/collect/${date}")
    }

    QName createCollectedFeedUri(id, updated, url) {
        def token = md5Hex(id.toString()+'@'+updated.toString()+'/'+url.toString())
        return new QName(systemBaseUri, "collect/${token}")
    }

    URI qnameToURI(QName qname) {
        return new URIImpl(qname.getNamespaceURI() + qname.getLocalPart())
    }

    // TODO: (optional) save *each* log method RDF to file
    // - requires init to make a dir for current collect
    void completeLogEvent() {
        collectEvent.setEnd(createXmlGrCal(new Date()))
        /*
        def repo = manager.repository
        def conn = repo.connection
        def ns = conn.&setNamespace
        ns("xsd", "http://www.w3.org/2001/XMLSchema#")
        ns("dct", "http://purl.org/dc/terms/")
        ns("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#")
        ns("iana", "http://www.iana.org/assignments/relation/")
        ns("tl", "http://purl.org/NET/c4dm/timeline.owl#")
        ns("rc", "http://rinfo.lagrummet.se/ns/2008/10/collector#")
        conn.close()
        RDFUtil.serialize(repo, "application/rdf+xml", System.out)
        */
    }
    // }}}

}
