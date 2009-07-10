package se.lagrummet.rinfo.store.depot

import org.junit.runner.RunWith
import spock.lang.*


@Speck @RunWith(Sputnik)
class FileDepotBatchTest {

    @Shared FileDepot depot

    def setup() {
        depot = (FileDepot) DepotUtil.depotFromConfig(
                "src/test/resources/rinfo-depot.properties");
    }

    def "should add and retrieve"() {
        setup:
        def id = "/publ/1901/100"
        def entry = depot.getEntry(id)
        def batch = depot.makeEntryBatch()
        assert batch.size() == 0

        when: batch.add(entry)
        then: batch.size() == 1
        when: batch.add(entry)
        then: batch.size() == 1

        when: batch.add(depot.getEntry("/publ/1901/100/revisions/1902/200"))
        then: batch.size() == 2
    }

    def "should fail on null"() {
        when:
        def batch = depot.makeEntryBatch()
        batch.add(null)
        then:
        thrown(NullPointerException)
    }

    def "should contain added entry"() {
        setup:
        def id = "/publ/1901/100"
        def entry = depot.getEntry(id)
        assert entry != null
        when:
        def batch = depot.makeEntryBatch()
        batch.add(entry)
        then:
        assert batch.contains(depot.getEntry(id))
    }

    def "should not contain not added entry"() {
        setup:
        def id = "/publ/1901/100"
        when:
        def entry = depot.getEntry(id)
        def batch = depot.makeEntryBatch()
        then:
        !batch.contains(depot.getEntry(id))
    }

}
