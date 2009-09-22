package se.lagrummet.rinfo.store.depot

import org.junit.runner.RunWith
import spock.lang.*


@Speck @RunWith(Sputnik)
class FileDepotTransactionTest {

    @Shared Depot depot
    @Shared def tdu = new TempDepotUtil()
    def setupSpeck() { depot = tdu.createTempDepot() }
    def cleanupSpeck() { tdu.deleteTempDepot() }

    def "should lock entry"() {
        setup:
        def id = "/publ/1901/100"
        def entry = depot.getEntry(id)
        when:
        entry.lock()

        then:
        assert entry.isLocked()

        when:
        depot.getEntry(id)
        then:
        thrown(LockedDepotEntryException)

        when:
        entry.unlock()
        then:
        !entry.isLocked()
        entry = depot.getEntry(id)
        !entry.isLocked()
    }

    def "should leave locked on bad content"() {
        setup:
        def id = new URI("http://example.org/publ/NEW/broken_1")

        when:
        def session = depot.openSession()
        session.createEntry(id, new Date(),
                [ new SourceContent(((InputStream)null), null, null) ])
        then:
        thrown(NullPointerException)

        when:
        session.close()
        depot.getEntry(id)

        then:
        thrown(LockedDepotEntryException)

        when:
        def brokenEntry = depot.backend.getUncheckedDepotEntry(id.path)
        then:
        assert brokenEntry.isLocked()
    }


    def "should wipe on rollback new"() {
        setup:
        def id = new URI("http://example.org/publ/NEW/rollback_1")

        when:
        def session = depot.openSession()
        def entry = session.createEntry(id, new Date(),
                [ new SourceContent(tdu.exampleEntryFile("content-en.pdf"), "application/pdf", "en"),
                  new SourceContent(tdu.exampleEntryFile("content.rdf"), "application/rdf+xml") ])
        session.close()
        then:
        depot.getEntry(id) != null

        when:
        entry.rollback()
        then:
        depot.getEntry(id) == null
        // TODO: IllegalStateException on any futher entry ops..
    }


    def "should unupdate on rollback updated"() {
        setup:
        def id = new URI("http://example.org/publ/UPD/rollback_1")
        def createTime = new Date()
        when:
        def session = depot.openSession()
        def entry = session.createEntry(id, createTime, [
                    new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")
                ], [
                    new SourceContent(tdu.exampleFile("icon.png"),
                            null, null, "images/icon.png"),
                ]
            )
        session.close()
        then:
        !entry.hasHistory()

        when:
        def updateTime = new Date(createTime.time+1000)
        entry = depot.getEntry(id)
        entry.update(updateTime,
                [ new SourceContent(tdu.exampleEntryFile("content-en.pdf"), "application/pdf", "en"),
                  new SourceContent(tdu.exampleEntryFile("content.rdf"), "application/rdf+xml") ],
                [ new SourceContent(tdu.exampleFile("icon.png"), null, null, "icon.png"),
                  new SourceContent(tdu.exampleFile("icon.png"), null, null, "images/icon.png") ]
            )
        then:
        assert entry.hasHistory()
        entry.updated == updateTime
        entry.findContents().size() == 2
        entry.findEnclosures().size() == 2

        when:
        def storedModified = entry.lastModified()
        entry.getMetaFile("TEST_META_FILE").setText("TEST")
        then:
        assert entry.getMetaFile("TEST_META_FILE").exists()

        when:
        Thread.sleep(1000) // to get newer fs timestamps..
        entry = depot.getEntry(id)
        entry.update(new Date(), [
                    new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")
                ], [
                    new SourceContent(tdu.exampleFile("icon.png"),
                            null, null, "images/icon.png"),
                ]
            )
        then:
        entry.findContents().size() == 1
        entry.findEnclosures().size() == 1
        updateTime < entry.updated
        storedModified < entry.lastModified()
        !entry.getMetaFile("TEST_META_FILE").exists()
        assert entry.hasHistory()

        when:
        entry.rollback()
        entry = depot.getEntry(id)
        then:
        entry.updated == updateTime
        entry.findContents().size() == 2
        entry.lastModified() == storedModified
        entry.findEnclosures().size() == 2
        assert entry.getMetaFile("TEST_META_FILE").exists()
        // TODO:IMPROVE: verify path of enclosures..
    }

}
