package se.lagrummet.rinfo.collector.atom

import org.junit.runner.RunWith
import spock.lang.*

import org.apache.abdera.model.Entry


@Speck @RunWith(Sputnik)
class AtomDetailsSpec {

    /* FIXME: Works with Abdera trunk, which is in no mvn repo yet (2009-10)!
    def "should handle links with unescaped colon in path"() {
        setup:
        def feedFile = new File("src/test/resources/feed/index.atom")
        when:
        def feed = FeedArchiveReader.parseFeed(new FileInputStream(feedFile),
                new URL("http://example.org/feed/index.atom"))
        then:
        feed.entries.each { entry ->
            def cElem = entry.contentElement
            if (cElem.mimeType.toString() == "application/rdf+xml") {
                assert "${entry.id}/rdf" == cElem.resolvedSrc.toString()
                assert cElem.resolvedSrc.toString().contains("entry:")
            }
            entry.links.each {
                assert it.resolvedHref.toString().contains("entry:")
            }
        }
    }
    */

}
