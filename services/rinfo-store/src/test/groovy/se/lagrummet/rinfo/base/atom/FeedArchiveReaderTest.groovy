package se.lagrummet.rinfo.base.atom

import org.junit.Test

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry


class FeedArchiveReaderTest {

    @Test(expected=MalformedURLException) // TODO: visit proper feed
    void shouldReadFeed() {
        def testFeedUrlPath = null
        def reader = new DummyFeeder()
        reader.readFeed new URL(testFeedUrlPath)
        assertEquals 1, reader.visitedPages
        // TODO: assert.. visited?
    }

}

class DummyFeeder extends FeedArchiveReader {

    int visitedPages

    boolean processFeedPage(URL pageUrl, Feed feed) {
        visitedPages += 1
        return false
    }

}
