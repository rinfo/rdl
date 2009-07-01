package se.lagrummet.rinfo.main.storage

import org.junit.runner.RunWith; import spock.lang.*

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore

import org.apache.abdera.Abdera
import org.apache.abdera.model.*
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.DepotEntryBatch


@Speck @RunWith(Sputnik)
class StorageSessionSpeck {

    Repository repo

    def setup() {
        repo = new SailRepository(new MemoryStore())
    }

    def makeStorageSession(depot, handlers) {
        depot.makeEntryBatch() >> { new DepotEntryBatch() }
        def storage = new Storage(depot, repo)
        storage.storageHandlers = handlers
        storage.startup()
        return storage.newStorageSession()
    }

    def "an entry is created"() {
        setup: "create mock depot and handler, create session"
        Depot depot = Mock()
        StorageHandler handler = Mock()
        StorageSession session = makeStorageSession(depot, [handler])

        and: "create dummy atom source"
        def sourceUrl = new URL("http://example.org/feed/current")
        Feed sourceFeed = Abdera.instance.newFeed()
        sourceFeed.id = new IRI("tag:example.org,2009:publ")
        sourceFeed.updated = new Date()
        def entryId = new URI("http://example.org/entry/1")
        def entryUpdated = new Date()
        Entry sourceEntry = Abdera.instance.newEntry()
        sourceEntry.id = new IRI(entryId)
        sourceEntry.updated = entryUpdated

        and: "mock created depotEntry"
        DepotEntry depotEntry = Mock()
        depotEntry.getId() >> entryId
        File metaFile = File.createTempFile("rinfomain", "metafile")
        depotEntry.getMetaFile(_) >> metaFile
        1 * depot.getEntry(entryId) >> null
        1 * depot.createEntry(entryId, _, _, _, _) >> depotEntry

        when: "an entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        session.storeEntry(sourceFeed, sourceEntry, [], [])
        session.endPage()

        then: "handlers are invoked"
        1 * handler.onCreate(session, depotEntry)

        and: "the write is logged"
        session.hasCollected(sourceEntry) == true
        // TODO: should log in .. feed?

        cleanup:
        session.close()
        metaFile.delete()
    }

    def "an entry is updated"() {
    }

    def "an entry is deleted"() {
    }

    def "a malformed entry is written"() {
    }

    def "network error for entry"() {
    }

    def "sessions are logged"() {
        def feed = null
        //session.logCollect(feed)
    }

}
