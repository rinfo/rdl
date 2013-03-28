package se.lagrummet.rinfo.store.depot

import spock.lang.*


class FileDepotWriteSpec extends Specification {

    @Shared Depot depot
    @Shared def tdu = new TempDepotUtil()
    def setupSpec() {
        depot = tdu.createTempDepot()
    }
    def cleanupSpec() { tdu.deleteTempDepot() }

    def setup() {
        // TODO: mock cleaner interface, assert incoming!
        //depot.atomizer = Mock(Atomizer)
    }

    @Shared currentDate =  new Date()
    private nextDate() {
        currentDate = new Date(currentDate.time+1000)
        return currentDate
    }

    def "should create entry"() {
        setup:
        def id = new URI("http://example.org/publ/NEW/added_1")
        assert !depot.hasEntry(id)
        when:
        def createTime = nextDate()
        def session = depot.openSession()
        def entry = session.createEntry(id, createTime,
                [ new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
                  new SourceContent(tdu.exampleEntryFile("content.rdf"),
                            "application/rdf+xml") ])
        //entry.commit()
        session.close()
        entry = depot.getEntry(id)
        then:
        !entry.isLocked()
        and:
        entry.id == id
        entry.published == entry.updated
        entry.updated == createTime
        !entry.isDeleted()
        and:
        entry.findContents("application/pdf", "en")[0] != null
        entry.findContents("application/rdf+xml")[0] != null
    }


    def "should create entry with enclosures"() {
        setup:
        def id = new URI("http://example.org/publ/NEW/added_2")
        assert depot.getEntry(id) == null
        when:
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [],
                [
                    // full path
                    new SourceContent(tdu.exampleFile("icon.png"),
                            null, null,
                            "/publ/NEW/added_2/icon.png"),
                    // relative to entry path
                    new SourceContent(tdu.exampleFile("icon.png"),
                            null, null,
                            "icon2.png"),
                    // nested path
                    new SourceContent(tdu.exampleFile("icon.png"),
                            null, null,
                            "images/icon.png"),
                ])
        session.close()
        def entry = depot.getEntry(id)
        def enclosures = entry.findEnclosures()
        then:
        enclosures.size() == 3
        and:
        ["/publ/NEW/added_2/icon.png",
         "/publ/NEW/added_2/icon2.png",
         "/publ/NEW/added_2/images/icon.png"
        ].each { path ->
            def encl = enclosures.find { it.depotUriPath == path }
            assert encl != null
            assert encl.mediaType == "image/png"
        }
    }

    def "should create locked entry"() {
        setup:
        def id = new URI("http://example.org/publ/NEW/added_3")
        assert depot.getEntry(id) == null
        when:
        def createTime = nextDate()
        def session = depot.openSession()
        def entry = session.createEntry(id, createTime,
                [ new SourceContent(tdu.exampleEntryFile("content.rdf"),
                            "application/rdf+xml") ])
        then:
        assert entry.isLocked()
        //entry.unlock()
        //!entry.isLocked()

        when:
        session.close()
        entry = depot.getEntry(id)
        then:
        entry != null
        !entry.isLocked()
    }

    def "should fail on creating existing entry"() {
        setup:
        def id = new URI("http://example.org/publ/1901/100")
        when:
        def session = depot.openSession()
        session.createEntry(id, nextDate(),
                [new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en")])
        then:
        thrown(DuplicateDepotEntryException)
        // TODO: should auto-rollback?
    }

    def "should fail on duplicate content"() {
        setup:
        def id = new URI("http://example.org/publ/NEW/added_4")
        def content = new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                "application/pdf", "en")
        when:
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [content, content])
        then:
        thrown(DuplicateDepotContentException)
        // TODO: should auto-rollback?
    }


    def "should fail on enclosure out of path"() {
        setup:
        def id = new URI("http://example.org/publ/ERROR/encl_1")
        def invalidEnclPath = "/publ/OTHER/path/icon.png"
        when:
        def session = depot.openSession()
        session.createEntry(id, nextDate(),
                [],
                [ new SourceContent(tdu.exampleFile("icon.png"),
                            null, null, invalidEnclPath), ]
            )
        then:
        thrown(DepotUriException)
    }


    def "should update entry"() {
        setup:
        def id = new URI("http://example.org/publ/UPD/updated_1")
        assert depot.getEntry(id) == null

        when:
        def createTime = nextDate()
        def session = depot.openSession()
        session.createEntry(id, createTime, [
                new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])
        session.close()
        def entry = depot.getEntry(id)
        then:
        entry != null
        entry.findContents("application/pdf").size() == 1
        entry.published == entry.updated
        entry.updated == createTime

        when:
        def updateTime = nextDate()
        session = depot.openSession()
        session.update(entry, updateTime, [
                new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
                new SourceContent(tdu.exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv")
            ])
        then:
        session.close()
        entry.findContents("application/pdf").size() == 2
        entry.updated > entry.published
        entry.published == createTime
        entry.updated == updateTime
        !entry.isDeleted()
        // TODO: getHistoricalEntries..
        !entry.isLocked()
    }


    def "should update entry with less contents"() {
        setup:
        def id = new URI("http://example.org/publ/UPD/updated_2")
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [
                new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
                new SourceContent(tdu.exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv")
            ])
        session.close()
        when:
        def entry = depot.getEntry(id)
        session = depot.openSession()
        session.update(entry, nextDate(), [
                new SourceContent(tdu.exampleEntryFile("content-sv.pdf"),
                            "application/pdf", "sv"),
            ])
        session.close()
        then:
        entry.contentLanguage == "sv"
        def contents = entry.findContents("application/pdf")
        contents.size() == 1
        contents[0].lang == "sv"
    }

    def "should move enclosures when updating entry"() {
        setup:
        def id = new URI("http://example.org/publ/UPD/updated_3")
        def session = depot.openSession()
        session.createEntry(id, nextDate(),
                [new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en")],
                [
                    new SourceContent(tdu.exampleFile("icon.png"),
                            null, null,
                            "icon2.png"),
                    new SourceContent(tdu.exampleFile("icon.png"),
                            null, null,
                            "images/icon.png"),
                ]
            )
        session.close()
        when:
        def entry = depot.getEntry(id)
        session = depot.openSession()
        session.update(entry, nextDate(), [
                new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                            "application/pdf", "en"),
            ])
        session.close()
        def enclosures = entry.findEnclosures()
        then:
        enclosures.size() == 0
    }

    def "should keep nested entry when updating entry"() {
        setup:
        def id = new URI("http://example.org/publ/UPD/updated_4")
        def nestedId = new URI("http://example.org/publ/UPD/updated_4/nested_1")
        when:
        def session = depot.openSession()
        session.createEntry(id, nextDate(),
                [new SourceContent(tdu.exampleEntryFile("content.rdf"),
                            "application/rdf+xml")],
                [new SourceContent(tdu.exampleFile("icon.png"),
                            null, null, "icon_1.png"),
                 new SourceContent(tdu.exampleFile("icon.png"),
                            null, null, "sub/icon_2.png")])
        session.createEntry(nestedId, nextDate(),
                [new SourceContent(tdu.exampleEntryFile("content.rdf"),
                            "application/rdf+xml")],
                [new SourceContent(tdu.exampleFile("icon.png"),
                            null, null, "icon_3.png"),
                new SourceContent(tdu.exampleFile("icon.png"),
                            null, null, "sub/icon_4.png")])
        and:
        session.update(depot.getEntry(id), nextDate(), [
                new SourceContent(tdu.exampleEntryFile("content.rdf"),
                            "application/rdf+xml"),
            ])
        session.close()
        then:
        def entry = depot.getEntry(id)
        entry.findContents("application/rdf+xml").size() == 1
        entry.findEnclosures().size() == 0
        and:
        def nestedEntry = depot.getEntry(nestedId)
        nestedEntry.findContents("application/rdf+xml").size() == 1
        nestedEntry.findEnclosures().size() == 2
    }

    def "should delete entry"() {
        setup:
        def id = new URI("http://example.org/publ/DEL/deleted_1")
        assert depot.getEntry(id) == null
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [
                new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])
        session.close()

        when:
        session = depot.openSession()
        def deleteTime = nextDate()
        def entry = depot.getEntry(id)
        session.delete(entry, deleteTime)
        session.close()
        then:
        def entry2 = depot.getEntryOrDeletedEntry(id.path)
        !entry2.isLocked()
        entry2.findContents("application/pdf").size() == 0
        entry2.updated == deleteTime
        assert entry2.isDeleted()

        when:
        depot.getEntry(id)
        then:
        thrown(DeletedDepotEntryException)
    }

    def "should be able ro resurrect deleted entry"() {
        given: "a deleted entry"
        def id = new URI("http://example.org/publ/DEL/deleted_2")
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [
                new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])
        session.close()
        session = depot.openSession()
        def deleteTime = nextDate()
        def entry = depot.getEntry(id)
        session.delete(entry, deleteTime)
        session.close()

        when: "entry is unconditionally retrieved"
        entry = depot.getEntryOrDeletedEntry(id.path)
        then: "it has a deleted state"
        assert entry.isDeleted()

        when: "a deleted entry is resurrected"
        entry.resurrect()
        def newCreateDate = nextDate()
        session = depot.openSession()
        session.createEntry(id, newCreateDate, [
                new SourceContent(tdu.exampleEntryFile("content-en.pdf"),
                        "application/pdf", "en"),
            ])
        session.close()
        then: "it appears as new"
        def session2 = depot.openSession()
        def entry2 = depot.getEntry(id)
        !entry2.isDeleted()
        entry2.published == newCreateDate
        entry2.updated == newCreateDate
    }

    def "should create entry and check md5 and length"() {
        setup:
        def id = new URI("http://example.org/publ/CHECK/${slug}")
        when:
        def srcContent = new SourceContent(
                tdu.exampleEntryFile("content-en.pdf"), "application/pdf", "en")
        srcContent.datachecks[SourceContent.Check.LENGTH] = new Long(24014)
        srcContent.datachecks[SourceContent.Check.MD5] = checksum
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [srcContent])
        session.close()
        then:
        notThrown(SourceCheckException)
        where:
        slug        | checksum
        "added_1"   | "eff60b86aaaac3a1fde5affc07a27006"
        "added_2"   | "EFF60B86AAAAC3A1FDE5AFFC07A27006"
    }

    def "should fail create entry on bad md5"() {
        setup:
        def id = new URI("http://example.org/publ/CHECK/failed_1")
        when:
        def srcContent = new SourceContent(
                tdu.exampleEntryFile("content-en.pdf"), "application/pdf", "en")
        srcContent.datachecks[SourceContent.Check.MD5] = "BAD_CHECKSUM"
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [srcContent])
        then:
        thrown(SourceCheckException)
    }

    def "should fail create entry on bad length"() {
        setup:
        def id = new URI("http://example.org/publ/CHECK/failed_2")
        when:
        def srcContent = new SourceContent(
                tdu.exampleEntryFile("content-en.pdf"), "application/pdf", "en")
        srcContent.datachecks[SourceContent.Check.LENGTH] = new Long(0)
        def session = depot.openSession()
        session.createEntry(id, nextDate(), [srcContent])
        then:
        thrown(SourceCheckException)
    }

}
