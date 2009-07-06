package se.lagrummet.rinfo.store.depot

import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.abdera.model.*
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed

import org.junit.runner.RunWith
import spock.lang.*


@Speck @RunWith(Sputnik)
class AtomizerTest {

    def atomizer = new Atomizer()

    def "should use feed skeleton resource"() {
        when: "using file location"
        String path = "src/test/resources/test_feed_skeleton.atom"
        atomizer.setFeedSkeletonPath(path)
        then:
        atomizer.feedSkeletonPath == path
        atomizer.feedSkeleton.title == "Test Feed"

        when: "using (class) resource location"
        atomizer.setFeedSkeletonPath("test_feed_skeleton.atom")
        then:
        atomizer.feedSkeleton.title == "Test Feed"

        when: "set Feed directly"
        def feed = Abdera.getInstance().newFeed()
        atomizer.setFeedSkeleton(feed)
        then:
        atomizer.feedSkeletonPath == null
        atomizer.feedSkeleton == feed
    }

}
