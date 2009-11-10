package se.lagrummet.rinfo.main.storage

import org.junit.runner.RunWith; import spock.lang.*

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Link

import se.lagrummet.rinfo.store.depot.DefaultPathHandler
import se.lagrummet.rinfo.main.storage.FeedCollector
import se.lagrummet.rinfo.main.storage.StorageSession
import static se.lagrummet.rinfo.main.storage.EntryPathMapper.DCT_IS_FORMAT_OF


@Speck @RunWith(Sputnik) class EntryPathMapperSpeck {

    static abdera = Abdera.instance
    EntryPathMapper epm

    def setup() {
        epm = new EntryPathMapper(new DefaultPathHandler())
    }

    def "should get enclosure slug from link"() {
        expect:
        epm.getEnclosureSlug(link) == expectedSlug
        where:
        link << [
            makeLink("http://example.org/item/one",
                    "http://localhost/item/one/file.txt"),
            //makeLink("http://example.org/item/one",
            //        "http://localhost/files.cgi?id=123",
            //        "file.txt"),
            makeLink("http://example.org/item/one",
                    "http://localhost/files.cgi?id=123",
                    "/item/one/file", "application/pdf"),
            makeLink("http://example.org/item/one",
                    "http://localhost/files.cgi?id=123",
                    "/item/one#file", "application/pdf"),
            //makeLink("http://example.org/item/one",
            //        "http://localhost/files.cgi?id=123",
            //        "css/style.css"),
            //makeLink("http://example.org/item/one",
            //        "http://localhost/files.cgi?id=123",
            //        "/item/one/css/style.css", "text/css"),
        ]
        expectedSlug << [
            "/item/one/file.txt",
            //"/item/one/file.txt",
            "/item/one/file.pdf",
            "/item/one/file.pdf",
            //"/item/one/css/style.css",
            //"/item/one/css/style.css",
        ]
    }

    def "should compute enclosure slug from URLs"() {
        expect:
        computedSlug == expectedSlug
        where:
        computedSlug << [
            epm.computeEnclosureSlug(new URI("http://example.org/item/one"),
                new URI("http://localhost/item/one/file.txt"))
        ]
        expectedSlug << [ "/item/one/file.txt" ]
    }

    private Link makeLink(entryId, href, ext=null, mtype=null) {
        def entry = abdera.newEntry()
        entry.setId(entryId)
        def link = entry.addLink(href)
        if (ext) {
            if (mtype)
                link.setMimeType(mtype)
            link.setAttributeValue(DCT_IS_FORMAT_OF, ext)
        }
        return link
    }

}
