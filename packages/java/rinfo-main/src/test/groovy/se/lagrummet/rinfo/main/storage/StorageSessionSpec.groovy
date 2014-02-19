package se.lagrummet.rinfo.main.storage

import spock.lang.*

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore

import org.apache.abdera.Abdera
import org.apache.abdera.model.*
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.DepotSession
import se.lagrummet.rinfo.store.depot.DepotEntry


class StorageSessionSpec extends Specification {

    StorageSession session

    Repository repo

    Feed sourceFeed
    Entry sourceEntry
    def sourceUrl = new URL("http://example.org/feed/current")
    def entryId = new URI("http://example.org/entry/1")

    def setup() {
        setup: "required components"
        repo = new SailRepository(new MemoryStore())
        repo.initialize()

        and: "create dummy atom source"
        sourceFeed = Abdera.instance.newFeed()
        sourceFeed.setId("tag:example.org,2009:publ")
        sourceFeed.setUpdated(new Date())
        def entryUpdated = new Date()
        sourceEntry = Abdera.instance.newEntry()
        sourceEntry.setId(entryId.toString())
        sourceEntry.setUpdated(entryUpdated)
        sourceEntry.setPublished(entryUpdated)
    }

    def cleanup() {
        session.close()
    }

    def "session credentials indicate admin rights"() {
        when:
        session = makeStorageSession(
                Mock(Depot), Mock(DepotSession), [], isAdmin)
        then:
        session.credentials.isAdmin() == isAdmin
        where:
        isAdmin << [true, false]
    }

    def "entry does not exist"() {
        when:
        session = makeStorageSession(Mock(Depot), Mock(DepotSession), [])
        then:
        session.hasCollected(sourceEntry) == false
    }

    def "an entry is created"() {
        setup:
        Depot depot = Mock()
        DepotSession depotSession = Mock()
        StorageHandler handler = Mock()
        session = makeStorageSession(depot, depotSession, [handler])

        and: "mock depot creation and subsequent retrieval"
        DepotEntry depotEntry = Mock()
        mockupDepotEntry(depotEntry)
        def entries = [:]
        2 * depot.getEntryOrDeletedEntry(entryId) >> { entries[entryId] }
        1 * depotSession.createEntry(entryId, _, _, _) >> {
            entries[entryId] = depotEntry
            return depotEntry
        }

        when: "a new entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        session.storeEntry(sourceFeed, sourceEntry, [], [])
        session.endPage()

        then:
        1 * handler.onModified(session, depotEntry, true)

        and: "the write is logged"
        session.hasCollected(sourceEntry) == true
        // TODO: log in .. eventRegistry?
    }

    def "skip entry and continue collect"() {
        setup:
        Depot depot = Mock()
        DepotSession depotSession = Mock()
        StorageHandler handler = Mock()
        CollectorLogSession logSession = Mock()
        session = makeStorageSessionWithLogSession(depot, depotSession, [handler], logSession)

        and: "mock depot creation and throw exception upon validation"
        DepotEntry depotEntry = Mock()
        mockupDepotEntry(depotEntry)
        def entries = [:]
        1 * depot.getEntryOrDeletedEntry(entryId) >> { entries[entryId] }
        1 * depotSession.createEntry(entryId, _, _, _) >> {
            entries[entryId] = depotEntry
            return depotEntry
        }
        handler.onModified(session, depotEntry, true) >> {
            throw new Exception()
        }
        logSession.logError(_, _, _, _) >> ErrorAction.SKIPANDCONTINUE

        when: "a new entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        boolean okToContinue = session.storeEntry(sourceFeed, sourceEntry, [], [])
        session.endPage()

        then: "the entry is rollbacked"
        1 * depotSession.rollbackPending()

        and: "the collect will continue"
        okToContinue
    }

    def "store entry and continue collect"() {
        setup:
        Depot depot = Mock()
        DepotSession depotSession = Mock()
        StorageHandler handler = Mock()
        CollectorLogSession logSession = Mock()
        session = makeStorageSessionWithLogSession(depot, depotSession, [handler], logSession)

        and: "mock depot creation and throw exception upon validation"
        DepotEntry depotEntry = Mock()
        mockupDepotEntry(depotEntry)
        def entries = [:]
        1 * depot.getEntryOrDeletedEntry(entryId) >> { entries[entryId] }
        1 * depotSession.createEntry(entryId, _, _, _) >> {
            entries[entryId] = depotEntry
            return depotEntry
        }
        handler.onModified(session, depotEntry, true) >> {
            throw new Exception()
        }
        logSession.logError(_, _, _, _) >> ErrorAction.STOREANDCONTINUE

        when: "a new entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        boolean okToContinue = session.storeEntry(sourceFeed, sourceEntry, [], [])
        session.endPage()

        then: "the entry is not rollbacked"
        0 * depotSession.rollbackPending()

        and: "the collect will continue"
        okToContinue
    }

    def "skip entry and stop collect"() {
        setup:
        Depot depot = Mock()
        DepotSession depotSession = Mock()
        StorageHandler handler = Mock()
        CollectorLogSession logSession = Mock()
        session = makeStorageSessionWithLogSession(depot, depotSession, [handler], logSession)

        and: "mock depot creation and throw exception upon validation"
        DepotEntry depotEntry = Mock()
        mockupDepotEntry(depotEntry)
        def entries = [:]
        1 * depot.getEntryOrDeletedEntry(entryId) >> { entries[entryId] }
        1 * depotSession.createEntry(entryId, _, _, _) >> {
            entries[entryId] = depotEntry
            return depotEntry
        }
        handler.onModified(session, depotEntry, true) >> {
            throw new Exception()
        }
        logSession.logError(_, _, _, _) >> ErrorAction.SKIPANDHALT

        when: "a new entry is written"
        session.beginPage(sourceUrl, sourceFeed)
        boolean okToContinue = session.storeEntry(sourceFeed, sourceEntry, [], [])
        session.endPage()

        then: "the entry is rollbacked"
        1 * depotSession.rollbackPending()

        and: "the collect will not continue"
        !okToContinue
    }



    def "an entry is updated"() {
        setup:
        Depot depot = Mock()
        DepotSession depotSession = Mock()
        StorageHandler handler = Mock()
        session = makeStorageSession(depot, depotSession, [handler])

        and: "mock depot retrieval and update"
        DepotEntry depotEntry = Mock()
        mockupDepotEntry(depotEntry)
        1 * depot.getEntryOrDeletedEntry(entryId) >> depotEntry
        1 * depotSession.update(depotEntry, _, _, _)

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
        1 * handler.onModified(session, depotEntry, false)

        and: "the write is logged"
        // TODO: log in .. eventRegistry?
    }

    def "an entry is deleted"() {
        setup:
        Depot depot = Mock()
        DepotSession depotSession = Mock()
        StorageHandler handler = Mock()
        session = makeStorageSession(depot, depotSession, [handler])

        and: "mock depot retrieval and deletion"
        DepotEntry depotEntry = Mock()
        def deletedDate = new Date()
        depotEntry.getId() >> entryId
        depotEntry.getUpdated() >> deletedDate
        1 * depot.getEntryOrDeletedEntry(entryId) >> depotEntry
        1 * depotSession.delete(depotEntry, _)

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

    private makeStorageSession(depot, depotSession, handlers, admin=false) {
        depot.openSession() >> depotSession
        depotSession.getDepot() >> depot
        def collectorLog = new CollectorLog(repo,
                "http://example.org/report/", "http://example.org/dataset/")
        def storage = new Storage(depot, collectorLog, null)
        storage.storageHandlers = handlers
        storage.startup()
        def sourceFeed = "http://example.org/feed"
        return storage.openSession(new StorageCredentials(
                    new CollectorSource(new URI(sourceFeed), new URL(sourceFeed)), admin))
    }

    private makeStorageSessionWithLogSession(depot, depotSession, handlers, admin=false, logSession) {
        depot.openSession() >> depotSession
        depotSession.getDepot() >> depot
        def sourceFeed = "http://example.org/feed"
        def credentials = new StorageCredentials(
            new CollectorSource(new URI(sourceFeed), new URL(sourceFeed)), admin)
        return new StorageSession(credentials, depotSession, handlers, logSession, null)
    }

    private mockupDepotEntry(depotEntry) {
        depotEntry.getId() >> entryId
        depotEntry.getPublished() >> new Date()
        depotEntry.getUpdated() >> new Date()
        def metaBytes = [:]
        depotEntry.getMetaOutputStream(_) >> { it
            def bos = new ByteArrayOutputStream()
            metaBytes[it] = bos
            return bos
        }
        depotEntry.getMetaInputStream(_) >> { it
            return new ByteArrayInputStream(metaBytes[it].toByteArray())
        }
    }
}
