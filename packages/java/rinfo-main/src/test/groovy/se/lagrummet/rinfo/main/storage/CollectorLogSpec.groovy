package se.lagrummet.rinfo.main.storage

import spock.lang.*

import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry

import se.lagrummet.rinfo.store.depot.DepotEntry


class CollectorLogSpec extends Specification {

    @Shared CollectorLog collectorLog
    CollectorLogSession logSession
    def sourceFeed

    def setupSpec() {
        def repo = new SailRepository(new MemoryStore())
        repo.initialize()
        collectorLog = new CollectorLog(repo,
                "http://example.org/report/", "http://example.org/dataset/")
    }

    def setup() {
        logSession = collectorLog.openSession()
        sourceFeed = Abdera.instance.newFeed()
        def feedPageUrl = new URL("http://data.example.org/feed/current.atom")
        sourceFeed.setId("tag:example.org,2009:publ")
        sourceFeed.setUpdated(new Date())
        logSession.start(new StorageCredentials(
                    new CollectorSource(sourceFeed.id.toURI(), feedPageUrl), false))
        logSession.logFeedPageVisit(feedPageUrl, sourceFeed)
    }

    def cleanup() {
        logSession.close()
    }

    def cleanupSpec() {
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
        def createTime = new Date()
        def entryId = "http://rinfo.lagrummet.se/publ/sfs/1999:175"
        def sourceEntry = Abdera.instance.newEntry()
        sourceEntry.setId(entryId)
        sourceEntry.setPublished(createTime)
        sourceEntry.setUpdated(createTime)

        DepotEntry depotEntry = Mock() // ..or from example files?
        depotEntry.getId() >> sourceEntry.id.toURI()
        depotEntry.getPublished() >> createTime
        depotEntry.getUpdated() >> createTime

        when:
        logSession.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)
        then:
        def entryDesc = logSession.state.pageDescriber.subjects(
                "rx:primarySubject", entryId).find {
                    it.getNative("awol:updated") == createTime
                }
        entryDesc.getNative("awol:published") == createTime
        entryDesc.getNative("awol:updated") == createTime
        entryDesc.getRel("dct:isPartOf").about == collectorLog.systemDatasetUri.toString()

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
