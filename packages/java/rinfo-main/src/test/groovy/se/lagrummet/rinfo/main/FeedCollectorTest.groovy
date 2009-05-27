package se.lagrummet.rinfo.main

//import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*


class FeedCollectorTest {

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
        assertEquals FeedCollector.getEnclosureSlug(
                new URI("http://example.org/item/one"),
                new URI("http://localhost/item/one/file.txt")),
                "/item/one/file.txt"
    }

    /* TODO:
    shouldSortUnsortedFeedPage
    shouldVerifyMd5AndLength

    shouldFailAndLogOnMissingRdf
    ...
    */

}
