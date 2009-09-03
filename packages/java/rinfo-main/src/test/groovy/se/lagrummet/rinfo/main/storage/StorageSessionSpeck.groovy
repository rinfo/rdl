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

    StorageSession session

    Repository repo

    Feed sourceFeed
    Entry sourceEntry
    def sourceUrl = new URL("http://example.org/feed/current")
    def entryId = new URI("http://example.org/entry/1")

    List<File> tempfiles

    def setup() {
        setup: "required components"
        repo = new SailRepository(new MemoryStore())

        and: "create dummy atom source"
        sourceFeed = Abdera.instance.newFeed()
        sourceFeed.setId("tag:example.org,2009:publ")
        sourceFeed.setUpdated(new Date())
        def entryUpdated = new Date()
        sourceEntry = Abdera.instance.newEntry()
        sourceEntry.setId(entryId.toString())
        sourceEntry.setUpdated(entryUpdated)
        sourceEntry.setPublished(entryUpdated)

        and:
        tempfiles = []
    }

    def cleanup() {
        session.close()
        tempfiles.each { it.delete() }
    }

    def makeStorageSession(depot, handlers, admin=false) {
        depot.makeEntryBatch() >> { new DepotEntryBatch() }
        def storage = new Storage(depot, repo)
        storage.storageHandlers = handlers
        storage.startup()
        return storage.openSession(new StorageCredentials(admin))
    }

    def "session credentials indicate admin rights"() {
        when:
        session = makeStorageSession(Mock(Depot), [], isAdmin)
        then:
        session.credentials.isAdmin() == isAdmin
        where:
        isAdmin << [true, false]
    }

    def "entry does not exist"() {
        when:
        session = makeStorageSession(Mock(Depot), [])
        then:
        session.hasCollected(sourceEntry) == false
    }

    def "an entry is created"() {
        setup:
        Depot depot = Mock()
        StorageHandler handler = Mock()
        session = makeStorageSession(depot, [handler])

        and: "mock depot creation and subsequent retrieval"
        DepotEntry depotEntry = Mock()
        depotEntry.getId() >> entryId
        depotEntry.getPublished() >> new Date()
        depotEntry.getUpdated() >> new Date()
        depotEntry.getMetaFile(_) >> tempFile("metafile")
        def entries = [:]
        2 * depot.getEntry(entryId) >> { entries[entryId] }
        1 * depot.createEntry(entryId, _, _, _, _) >> {
            entries[entryId] = depotEntry
        }

        when: "a new entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        session.storeEntry(sourceFeed, sourceEntry, [], [])
        session.endPage()

        then:
        1 * handler.onCreate(session, depotEntry)

        and: "the write is logged"
        session.hasCollected(sourceEntry) == true
        // TODO: log in .. eventRegistry?
    }

    def "an entry is updated"() {
        setup:
        Depot depot = Mock()
        StorageHandler handler = Mock()
        session = makeStorageSession(depot, [handler])

        and: "mock depot retrieval and update"
        DepotEntry depotEntry = Mock()
        depotEntry.getId() >> entryId
        depotEntry.getUpdated() >> new Date()
        depotEntry.getPublished() >> new Date()
        depotEntry.getMetaFile(_) >> tempFile("metafile")
        1 * depot.getEntry(entryId) >> depotEntry
        1 * depotEntry.update(_, _, _)

        and: "create existing meta-info for entry"
        session.setViaEntry(depotEntry, sourceFeed, sourceEntry)

        and: "create updated source"
        Entry updSourceEntry = sourceEntry.clone()
        updSourceEntry.setUpdated(new Date(sourceEntry.updated.time + 1))

        when: "an existing entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        session.storeEntry(sourceFeed, updSourceEntry, [], [])
        session.endPage()

        then:
        1 * handler.onUpdate(session, depotEntry)

        and: "the write is logged"
        // TODO: log in .. eventRegistry?
    }

    def "an entry is deleted"() {
        setup:
        Depot depot = Mock()
        StorageHandler handler = Mock()
        session = makeStorageSession(depot, [handler])

        and: "mock depot retrieval and deletion"
        DepotEntry depotEntry = Mock()
        def deletedDate = new Date()
        depotEntry.getId() >> entryId
        depotEntry.getUpdated() >> deletedDate
        1 * depot.getEntry(entryId) >> depotEntry
        1 * depotEntry.delete(_)

        when: "an existing entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        session.deleteEntry(sourceFeed, sourceEntry.id.toURI(), deletedDate)
        session.endPage()

        then:
        1 * handler.onDelete(session, depotEntry)

        and: "the delete is logged"
        // TODO: log in .. eventRegistry?
    }

    def "a malformed entry is written"() {
    }

    def "network error for entry"() {
    }

    def "sessions are logged"() {
        def feed = null
        //session.logCollect(feed)
    }

    private tempFile(suffix) {
        def f = File.createTempFile("rinfomain", suffix)
        tempfiles << f
        return f
    }

}
