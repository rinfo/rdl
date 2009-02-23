package se.lagrummet.rinfo.main

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*


class FeedCollectorTest {

    def value = null
    def given = { value = it }
    def expect = { assertEquals it, value }

    @BeforeClass
    static void setupClass() {
    }

    @AfterClass
    static void tearDownClass() {
    }

    @Test
    void shouldCollectNewSinceLast() {
    }

    @Test
    void shouldGetEnclosureSlug() {
        given FeedCollector.getEnclosureSlug(
                new URI("http://example.org/item/one"),
                new URI("http://localhost/item/one/file.txt"))
        expect "/item/one/file.txt"
    }

    /* TODO:
    shouldSortUnsortedFeedPage
    shouldVerifyMd5AndLength

    shouldFailAndLogOnMissingRdf
    ...
    */

}
