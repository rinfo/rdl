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
import org.openrdf.query.QueryLanguage
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection

import org.openrdf.elmo.ElmoManager
import org.openrdf.elmo.ElmoModule
import org.openrdf.elmo.sesame.SesameManagerFactory

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.DepotEntry

import se.lagrummet.rinfo.main.storage.log.CollectEvent
import se.lagrummet.rinfo.main.storage.log.FeedEvent
import se.lagrummet.rinfo.main.storage.log.EntryEvent
import se.lagrummet.rinfo.main.storage.log.DeletedEntryEvent
import se.lagrummet.rinfo.main.storage.log.ErrorEvent


// TODO: this is a "Session" thingy (stateful and should be closed); rename
// to.. FeedCollectorLogSession?
class FeedCollectorRegistry {

    private Repository repo;
    private RepositoryConnection conn;
    private ElmoManager manager;

    private CollectEvent collectEvent
    private FeedEvent currentFeedEvent // TODO: don't keep; use it's uri..

    private static ElmoModule module = new ElmoModule()
    static {
        module.addConcept(CollectEvent)
        module.addConcept(FeedEvent)
        module.addConcept(EntryEvent)
        module.addConcept(DeletedEntryEvent)
        module.addConcept(ErrorEvent)
    }

    FeedCollectorRegistry(Repository repo) {
        this.repo = repo;
        // FIXME: don't re-init for every session?
        def factory = new SesameManagerFactory(module, repo)
        factory.setQueryLanguage(QueryLanguage.SPARQL)
        this.conn = repo.getConnection();
        manager = factory.createElmoManager()
        def collectStartTime = new Date()
        collectEvent = manager.create(createCollectUri(), CollectEvent)
        collectEvent.setStart(createXmlGrCal(collectStartTime))
    }

    public Repository getRepo() { return repo; }
    public ElmoManager getManager() { return manager; }

    void close() {
        manager.close()
    }

    // TODO: configuration properties
    String sysUriBase = "http://rinfo.lagrummet.se/system/"
    URI entrySpaceUri = new URIImpl("tag:lagrummet.se,2009:rinfo")

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
        entryEvent.setAbout(new URIImpl(sourceEntry.getId().toString()))
        entryEvent.setSpace(entrySpaceUri)
        entryEvent.setViaEntry(sourceEntryEvent)
        completeLogEvent()
    }

    void logDeletedEntry(Feed sourceFeed,
            java.net.URI sourceEntryId,
            Date sourceEntryDeleted,
            DepotEntry depotEntry) {
        def deleted = manager.create(DeletedEntryEvent)
        deleted.setAbout(new URIImpl(sourceEntryId.toString()))
        // TODO: record sourceEntryDeleted in viaDeletedEntryEvent?
        deleted.setAt(createXmlGrCal(depotEntry.getUpdated()))
        deleted.setSpace(entrySpaceUri)
        deleted.setViaFeed(currentFeedEvent)
        completeLogEvent()
    }

    void logError(Exception error, Date timestamp,
            Feed sourceFeed, Entry sourceEntry) {
        def errorEvent = manager.create(ErrorEvent)
        errorEvent.setAt(createXmlGrCal(new Date()))
        def sourceEntryEvent = createSourceEntryEvent(sourceEntry)
        errorEvent.setViaEntry(sourceEntryEvent)
        // TODO: spec what and how to record errors
        errorEvent.setValue(error.getMessage())
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
        return new QName(sysUriBase, "event/collect/${date}")
    }

    QName createCollectedFeedUri(id, updated, url) {
        def token = md5Hex(id.toString()+'@'+updated.toString()+'/'+url.toString())
        return new QName(sysUriBase, "collect/${token}")
    }

    URI qnameToURI(QName qname) {
        return new URIImpl(qname.getNamespaceURI() + qname.getLocalPart())
    }

    // TODO: (optional) save *each* log method RDF to file
    // - requires init to make a dir for current collect
    void completeLogEvent() {
        /*
        def conn = repo.connection
        def ns = conn.&setNamespace
        ns("xsd", "http://www.w3.org/2001/XMLSchema#")
        ns("dct", "http://purl.org/dc/terms/")
        ns("sioc", "http://rdfs.org/sioc/ns#")
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