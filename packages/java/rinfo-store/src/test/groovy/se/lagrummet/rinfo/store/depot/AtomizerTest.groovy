package se.lagrummet.rinfo.store.depot

import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.abdera.model.*
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*


class AtomizerTest {

    def atomizer = new Atomizer()

    @Test
    void shouldUseFeedSkeletonResource() {
        // using file location
        String path = "src/test/resources/test_feed_skeleton.atom"
        atomizer.setFeedSkeletonPath(path)
        assertEquals path, atomizer.feedSkeletonPath
        assertEquals "Test Feed", atomizer.feedSkeleton.title
        // using (class) resource location
        atomizer.setFeedSkeletonPath("test_feed_skeleton.atom")
        assertEquals "Test Feed", atomizer.feedSkeleton.title
        // set Feed directly
        def feed = Abdera.getInstance().newFeed()
        atomizer.setFeedSkeleton(feed)
        assertNull atomizer.feedSkeletonPath
        assertEquals feed, atomizer.feedSkeleton
    }

}
