package se.lagrummet.rinfo.store.depot

import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.abdera.model.*
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.Abdera

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*


class AtomizerTest  {

    def atomizer = new Atomizer()

    @Before
    void setup() {
        atomizer.configure(new PropertiesConfiguration())
    }

    @Test
    void shouldGetDeletedMarkers() {
        def feed = (Feed) Abdera.instance.parser.parse(
                new FileInputStream("src/test/resources/feed_with_deleted.atom")
            ).root

        assertEquals(
                atomizer.getDeletedMarkers(feed),
                [
                    (new IRI("http://example.org/docs/fs/5")):
                            new AtomDate("2008-10-08T00:00:04.000Z"),
                    (new IRI("http://example.org/docs/fs/4")):
                            new AtomDate("2008-10-08T00:00:03.000Z"),
                    (new IRI("http://example.org/docs/fs/3")):
                            new AtomDate("2008-10-08T00:00:02.000Z"),
                    (new IRI("http://example.org/docs/fs/2")):
                            new AtomDate("2008-10-08T00:00:01.000Z"),
                    (new IRI("http://example.org/docs/fs/1")):
                            new AtomDate("2008-10-08T00:00:00.000Z")
                ]
            )
    }

}
