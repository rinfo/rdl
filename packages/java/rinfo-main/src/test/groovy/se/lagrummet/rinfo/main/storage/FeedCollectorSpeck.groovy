package se.lagrummet.rinfo.main.storage

import org.junit.runner.RunWith; import spock.lang.*

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Link

import static se.lagrummet.rinfo.main.storage.FeedCollector.LINK_EXT_FILENAME
import static se.lagrummet.rinfo.main.storage.FeedCollector.getEnclosureSlug
import static se.lagrummet.rinfo.main.storage.FeedCollector.computeEnclosureSlug


@Speck @RunWith(Sputnik) class FeedCollectorSpeck {

    static abdera = Abdera.instance

    def "should collect new since last"() {
    }

    def "should get enclosure slug from link"() {
        expect:
        getEnclosureSlug(link) == expectedSlug
        where:
        link << [
            makeLink("http://example.org/item/one",
                    "http://localhost/item/one/file.txt"),
            makeLink("http://example.org/item/one",
                    "http://localhost/files.cgi?id=123",
                    "file.txt"),
            makeLink("http://example.org/item/one",
                    "http://localhost/files.cgi?id=123",
                    "/item/one/file.txt"),
            makeLink("http://example.org/item/one",
                    "http://localhost/files.cgi?id=123",
                    "css/style.css"),
            makeLink("http://example.org/item/one",
                    "http://localhost/files.cgi?id=123",
                    "/item/one/css/style.css"),
        ]
        expectedSlug << [
            "/item/one/file.txt",
            "/item/one/file.txt",
            "/item/one/file.txt",
            "/item/one/css/style.css",
            "/item/one/css/style.css",
        ]
    }

    def "should compute enclosure slug from URLs"() {
        expect:
        computedSlug == expectedSlug
        where:
        computedSlug << [
            computeEnclosureSlug(new URI("http://example.org/item/one"),
                new URI("http://localhost/item/one/file.txt"))
        ]
        expectedSlug << [ "/item/one/file.txt" ]
    }

    private Link makeLink(entryId, href, ext=null) {
        def entry = abdera.newEntry()
        entry.setId(entryId)
        def link = entry.addLink(href)
        if (ext) link.setAttributeValue(LINK_EXT_FILENAME, ext)
        return link
    }

    /* TODO:
    shouldSortUnsortedFeedPage
    shouldVerifyMd5AndLength

    shouldFailAndLogOnMissingRdf
    ...
    */

}
