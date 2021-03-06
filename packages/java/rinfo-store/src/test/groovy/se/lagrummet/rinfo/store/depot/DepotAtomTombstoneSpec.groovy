package se.lagrummet.rinfo.store.depot

import org.apache.abdera.Abdera
import org.apache.abdera.model.AtomDate

import spock.lang.*


class DepotAtomTombstoneSpec extends Specification {

    @Shared Depot depot
    @Shared def tdu = new TempDepotUtil()
    def setupSpec() { depot = tdu.createTempDepot() }
    def cleanupSpec() { tdu.deleteTempDepot() }

    def "should, if configured, use entries as tombstones"() {
        when:
        def entry = getDeletedEntry()
        then:
        def feed = newFeedWithIndexedEntry(atomizer, entry)
        def found = feed.getEntry(entry.id as String)
        (!found || found.updated != entry.updated) == gotTombstoneEntry
        where:
        atomizer << [
            new Atomizer(
                useTombstones:false,
                useFeedSync:true,
                useGdataDeleted:false),
            new Atomizer(
                useTombstones:false,
                useFeedSync:false,
                useGdataDeleted:true),

            new Atomizer(
                useTombstones:true,
                useFeedSync:false,
                useGdataDeleted:false),
            new Atomizer(
                useTombstones:false,
                useFeedSync:false,
                useGdataDeleted:false)
        ]
        gotTombstoneEntry << [
            false, false,
            true, true
        ]
    }

    def "should, if configured, only use feed level tombstones"() {
        setup:
        def entry = getDeletedEntry()
        when:
        def feed = newFeedWithIndexedEntry(new Atomizer(
            useTombstones:true,
            useFeedSync:false,
            useGdataDeleted:false,
        ), entry)
        then:
        feed.getEntry(entry.id as String) == null
        and:
        def tombstones = feed.getExtensions(Atomizer.FEED_EXT_TOMBSTONE
                ).collect {
            [
                ref: it.getAttributeValue(Atomizer.TOMBSTONE_REF),
                when: new AtomDate(
                        it.getAttributeValue(Atomizer.TOMBSTONE_WHEN)).date
            ]
        }
        tombstones.find {
            it.ref == entry.id as String && it.when == entry.updated
        } != null
    }

    private newFeedWithIndexedEntry(atomizer, entry) {
        def feed = Abdera.getInstance().newFeed()
        atomizer.addEntryToFeed(entry, feed)
        return feed
    }

    private getDeletedEntry() {
        return depot.backend.getUncheckedDepotEntry("/publ/1901/0")
    }

}
