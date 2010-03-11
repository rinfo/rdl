package se.lagrummet.rinfo.main.storage

import spock.lang.*

import javax.xml.namespace.QName

import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.model.Resource
import org.openrdf.model.URI as RdfURI
import org.openrdf.model.impl.URIImpl

import org.openrdf.elmo.ElmoQuery

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.base.rdf.RDFUtil


import se.lagrummet.rinfo.main.storage.log.CollectEvent
import se.lagrummet.rinfo.main.storage.log.FeedEvent
import se.lagrummet.rinfo.main.storage.log.EntryEvent
import se.lagrummet.rinfo.main.storage.log.DeletedEntryEvent
import se.lagrummet.rinfo.main.storage.log.ErrorEvent


class CollectorLogSpeck extends Specification {

    @Shared CollectorLog collectorLog
    CollectorLogSession logSession

    def setupSpeck() {
        def repo = new SailRepository(new MemoryStore())
        collectorLog = new CollectorLog(repo)
    }

    def setup() {
        logSession = collectorLog.openSession()
    }

    def cleanup() {
        logSession.close()
    }

    def cleanupSpeck() {
        collectorLog.shutdown()
    }

    /*
    def "feed page visits are logged"() {
        when:
        logSession.logFeedPageVisit(pageUrl, feed)
        then:
        ...
    }
    */

    def "entries are logged"() {
        setup:
        def sourceFeed = Abdera.instance.newFeed()
        sourceFeed.setId("tag:example.org,2009:publ")
        sourceFeed.setUpdated(new Date())
        def time = new Date()
        def entryId = "http://rinfo.lagrummet.se/publ/sfs/1999:175"
        def sourceEntry = Abdera.instance.newEntry()
        sourceEntry.setId(entryId)
        sourceEntry.setPublished(time)
        sourceEntry.setUpdated(time)

        DepotEntry depotEntry = Mock() // ..or from example files?
        depotEntry.getId() >> sourceEntry.id.toURI()
        depotEntry.getPublished() >> time
        depotEntry.getUpdated() >> time

        when:
        logSession.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)

        then:
        //logSession.findEntryEvent(subject, updated, dataset)
        def query = logSession.manager.createQuery("""
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX rx: <http://www.w3.org/2008/09/rx#>
        PREFIX awol: <http://bblfish.net/work/atom-owl/2006-06-06/#>
        SELECT ?entry WHERE {
            ?entry a awol:Entry;
                rx:primarySubject ?primarySubject;
                dct:isPartOf ?dataset;
                awol:updated ?updated .
        } """)
        query.setParameter("primarySubject",
                logSession.manager.find(new QName(entryId, "")))
        query.setParameter("dataset",
                logSession.manager.find(new QName(collectorLog.entryDatasetUri.toString(), "")))
        query.setParameter("updated", logSession.createXmlGrCal(time))

        EntryEvent entryEvent = query.evaluate().collect { it }[0]
        entryEvent != null
        entryEvent.primarySubjectObject.toString() == entryId
        def toTime = { it.toGregorianCalendar().time }
        toTime(entryEvent.published) == time
        toTime(entryEvent.updated) == time
        entryEvent.isPartOfObject.toString() == collectorLog.entryDatasetUri.toString()

        //and:
        //// TODO: seems good to bundle get-/setViaEntry and EntryRdfReader as
        //// .. DepotEntryView(depotEntry) ..
        //// and also use in session+handlers..
        //Entry viaEntry = StorageSession.getViaEntry(depotEntry)
        //def via = object(conn, eventItem, VIA)
        //object(conn, via, UPDATED) == dateTime(viaEntry.updated)
        //and:
        //def viaSource = object(conn, via, SOURCE)
        //object(conn, viaSource, SELF) == viaEntry.source.selfLinkResolvedHref
    }

    /*
    def "deletions are logged"() {
        when:
        def sourceFeed = null
        def sourceEntryId = null
        def sourceEntryDeleted = null
        def depotEntry = null
        logSession.logDeletedEntry(sourceFeed, sourceEntryId, sourceEntryDeleted,
            depotEntry)
        then:
        ...
    }
    */

    def "errors are logged"() {
        logSession.logError(exception, timestamp, sourceFeed, sourceEntry)
    }

}
