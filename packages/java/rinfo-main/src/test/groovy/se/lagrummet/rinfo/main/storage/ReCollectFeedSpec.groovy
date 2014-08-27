package se.lagrummet.rinfo.main.storage

import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.model.Content
import spock.lang.Specification

class ReCollectFeedSpec extends Specification {
    def "It should produce a feed from a queue"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost")
            entry.setTitle("http://localhost")

            def content = Abdera.instance.factory.newContent()
            content.setSrc("http://loclahost/blaj")
            content.setContentType(Content.Type.MEDIA)
            entry.setContentElement(content)

            def dummy = entry.getContentElement()
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
        when:
            def feed = ReCollectFeed.generate(queue.asList)
        then:
            feed.getEntry("http://localhost").getContentSrc() == content.getSrc()

    }
}
