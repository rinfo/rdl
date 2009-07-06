package se.lagrummet.rinfo.store.depot

import org.junit.runner.RunWith
import spock.lang.*


@Speck @RunWith(Sputnik)
class FileDepotTransactionTest extends FileDepotTempBase {

    def setupSpeck() { createTempDepot() }
    def cleanupSpeck() { deleteTempDepot() }

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
        depot.createEntry(id, new Date(),
                [ new SourceContent(((InputStream)null), null, null) ])
        then:
        thrown(NullPointerException)

        when:
        depot.getEntry(id)

        then:
        thrown(LockedDepotEntryException)

        when:
        def brokenEntry = depot.getUncheckedDepotEntry(id.path)
        then:
        assert brokenEntry.isLocked()
    }


    def "should wipe on rollback new"() {
        setup:
        def id = new URI("http://example.org/publ/NEW/rollback_1")

        when:
        def entry = depot.createEntry(id, new Date(),
                [ new SourceContent(exampleEntryFile("content-en.pdf"), "application/pdf", "en"),
                  new SourceContent(exampleEntryFile("content.rdf"), "application/rdf+xml") ])
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
        def entry = depot.createEntry(id, createTime, [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")
                ], [
                    new SourceContent(exampleFile("icon.png"),
                            null, null, "images/icon.png"),
                ]
            )
        then:
        !entry.hasHistory()

        when:
        Thread.sleep(1000) // wait 1 sec to get file lastModified stamps correct..
        def updateTime = new Date()
        entry = depot.getEntry(id)
        entry.update(updateTime,
                [ new SourceContent(exampleEntryFile("content-en.pdf"), "application/pdf", "en"),
                  new SourceContent(exampleEntryFile("content.rdf"), "application/rdf+xml") ],
                [ new SourceContent(exampleFile("icon.png"), null, null, "icon.png"),
                  new SourceContent(exampleFile("icon.png"), null, null, "images/icon.png") ]
            )
        then:
        assert entry.hasHistory()
        entry.updated == updateTime
        entry.findContents().size() == 3
        entry.findEnclosures().size() == 2

        when:
        def storedModified = entry.lastModified()
        entry.getMetaFile("TEST_META_FILE").setText("TEST")
        then:
        assert entry.getMetaFile("TEST_META_FILE").exists()

        when:
        Thread.sleep(1000)
        entry = depot.getEntry(id)
        entry.update(new Date(), [
                    new SourceContent(exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")
                ], [
                    new SourceContent(exampleFile("icon.png"),
                            null, null, "images/icon.png"),
                ]
            )
        then:
        entry.findContents().size() == 2
        entry.findEnclosures().size() == 1
        updateTime < entry.updated
        storedModified < entry.lastModified()
        !entry.getMetaFile("TEST_META_FILE").exists()
        assert entry.hasHistory()

        when:
        Thread.sleep(1000)
        entry.rollback()
        entry = depot.getEntry(id)
        then:
        entry.updated == updateTime
        entry.findContents().size() == 3
        entry.lastModified() == storedModified
        entry.findEnclosures().size() == 2
        assert entry.getMetaFile("TEST_META_FILE").exists()
        // TODO:IMPROVE: verify path of enclosures..
    }

}
