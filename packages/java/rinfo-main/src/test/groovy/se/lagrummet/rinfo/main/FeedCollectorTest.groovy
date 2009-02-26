package se.lagrummet.rinfo.main

//import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*


class FeedCollectorTest {

    def value, given = { value = it }, expect = { assertEquals it, value }
    //def values = []
    //def given = { values << it }, expect = { assertEquals it, values.pop() }
    //@After void forget() { values.clear() }

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
