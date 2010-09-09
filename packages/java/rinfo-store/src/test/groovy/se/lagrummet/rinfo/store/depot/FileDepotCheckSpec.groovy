package se.lagrummet.rinfo.store.depot

import spock.lang.*


class FileDepotCheckSpec extends Specification {

    @Shared Depot depot = DepotUtil.depotFromConfig(
                "src/test/resources/rinfo-depot.properties")

    def "consistency check should detect locked entry"() {
        when:
        depot.checkConsistency()
        then:
        thrown(LockedDepotEntryException)
    }

}
