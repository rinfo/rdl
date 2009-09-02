package se.lagrummet.rinfo.main.storage

import org.junit.runner.RunWith; import spock.lang.*

import se.lagrummet.rinfo.store.depot.Depot
import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore


@Speck @RunWith(Sputnik)
class StorageSpeck {

    Depot depot = Mock()
    Repository repo = /*Mock()*/new SailRepository(new MemoryStore())
    StorageHandler handler1 = Mock()
    StorageHandler handler2 = Mock()
    Storage storage

    def setup() {
        storage = new Storage(depot, repo)
    }

    def "storage is created"() {
        when: "storage is created"
        assert storage
        then: "depot is exposed"
        storage.depot == depot
        //and: "initializations have occurred"
        //1 * repo.initialize()
    }

    def "storage startup"() {
        when:
        storage.storageHandlers = [handler1, handler2]
        storage.startup()
        then: "startup invoked storageHandlers"
        1 * handler1.onStartup(_ as StorageSession)
        1 * handler2.onStartup(_ as StorageSession)
    }

    def "storage shutdown"() {
        //when:
        //storage.shutdown()
        //then: "shutdowns have occurred"
        //1 * repo.shutDown()
    }

}
