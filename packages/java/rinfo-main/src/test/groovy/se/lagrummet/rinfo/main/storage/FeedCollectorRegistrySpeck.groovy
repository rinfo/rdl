package se.lagrummet.rinfo.main.storage

import org.junit.runner.RunWith; import spock.lang.*

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


@Speck @RunWith(Sputnik)
class FeedCollectorRegistrySpeck {

    FeedCollectorRegistry reg

    def setup() {
        def repo = new SailRepository(new MemoryStore())
        repo.initialize()
        reg = new FeedCollectorRegistry(repo)
    }

    def cleanup() {
        reg.close()
    }

    /*
    def "feed page visits are logged"() {
        when:
        reg.logFeedPageVisit(pageUrl, feed)
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
        reg.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)

        then:
        //reg.findEntryEvent(about, updated, space)
        def query = reg.manager.createQuery("""
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        PREFIX sioc: <http://rdfs.org/sioc/ns#>
        PREFIX awol: <http://bblfish.net/work/atom-owl/2006-06-06/#>
        SELECT ?entry WHERE {
            ?entry a awol:Entry;
                sioc:about ?about;
                sioc:has_space ?space;
                awol:updated ?updated .
        """ + "}")
        query.setParameter("about",
                reg.manager.find(new QName(entryId, "")))
        query.setParameter("space",
                reg.manager.find(new QName(reg.entrySpaceUri.toString(), "")))
        query.setParameter("updated", reg.createXmlGrCal(time))

        EntryEvent entryEvent = query.evaluate().collect { it }[0]
        entryEvent != null
        entryEvent.getAboutObject().toString() == entryId
        def toTime = { it.toGregorianCalendar().time }
        toTime(entryEvent.getPublished()) == time
        toTime(entryEvent.getUpdated()) == time
        entryEvent.getSpaceObject().toString() == reg.entrySpaceUri.toString()

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
        reg.logDeletedEntry(sourceFeed, sourceEntryId, sourceEntryDeleted,
            depotEntry)
        then:
        ...
    }
    */

    def "errors are logged"() {
        reg.logError(exception, timestamp, sourceFeed, sourceEntry)
    }

}
