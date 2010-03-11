package se.lagrummet.rinfo.main.storage

import spock.lang.*

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Link
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.DefaultPathHandler
import se.lagrummet.rinfo.main.storage.FeedCollector
import se.lagrummet.rinfo.main.storage.StorageSession
import static se.lagrummet.rinfo.main.storage.EntryPathMapper.DCT_IS_FORMAT_OF


class EntryPathMapperSpeck extends Specification {

    static abdera = Abdera.instance
    @Shared epm = new EntryPathMapper(new DefaultPathHandler())
    @Shared entryId = "http://example.org/item/one"
    @Shared contentSrc = "http://localhost/item/one/content.txt"

    def "should get enclosure slug from link"() {
        expect:
        epm.getEnclosureSlug(link) == expectedSlug
        where:
        link << [
            makeEntryLink(contentSrc, "http://localhost/item/one/file.txt"),
            //makeEntryLink(contentSrc,
            //        "http://localhost/files.cgi?id=123",
            //        "file.txt"),
            makeEntryLink(contentSrc, "http://localhost/files.cgi?id=123",
                    "/item/one/file", "application/pdf"),
            makeEntryLink(contentSrc, "http://localhost/files.cgi?id=123",
                    "/item/one#file", "application/pdf"),
            //makeEntryLink(contentSrc,
            //        "http://localhost/files.cgi?id=123",
            //        "css/style.css"),
            //makeEntryLink(contentSrc,
            //        "http://localhost/files.cgi?id=123",
            //        "/item/one/css/style.css", "text/css"),
            makeEntryLink("http://localhost/other/item/one/content.txt",
                    "http://localhost/other/item/one/file.txt",),
        ]
        expectedSlug << [
            "/item/one/file.txt",
            //"/item/one/file.txt",
            "/item/one/file.pdf",
            "/item/one/file.pdf",
            //"/item/one/css/style.css",
            //"/item/one/css/style.css",
            "/item/one/file.txt",
        ]
    }

    def "should compute enclosure slug from URL"() {
        expect:
        computedSlug == expectedSlug
        where:
        computedSlug << [
            epm.computeEnclosureSlug(new URI("http://example.org/item/one"),
                new URI("http://localhost/item/one/file.txt"))
        ]
        expectedSlug << [ "/item/one/file.txt" ]
    }

    def "should infer enclosure slug from URL"() {
        when:
        def basePath = "/sys/uri"
        def contentPath = "/admin/sys/uri/rdf"
        def enclosureHref = "/admin/sys/uri/scheme.rdf"
        then:
        epm.inferEnclosureSlug(basePath, contentPath, enclosureHref) ==
                "/sys/uri/scheme.rdf"
    }

    def "should find common base of two paths"() {
        when:
        def contentPath = "/admin/sys/uri/rdf"
        def enclosureHref = "/admin/sys/uri/scheme.rdf"
        def commonHrefBase = epm.findCommonBase(enclosureHref, contentPath)
        then:
        commonHrefBase == "/admin/sys/uri/"
        epm.substringAfter(contentPath, commonHrefBase) == "rdf"
        epm.substringAfter(enclosureHref, commonHrefBase) == "scheme.rdf"
    }

    private Link makeEntryLink(src, href, named=null, mtype=null) {
        def entry = abdera.newEntry()
        entry.setId(entryId)
        entry.setContent(new IRI(src), "text/plain")
        def link = entry.addLink(href)
        if (named) {
            if (mtype)
                link.setMimeType(mtype)
            link.setAttributeValue(DCT_IS_FORMAT_OF, named)
        }
        return link
    }

}
