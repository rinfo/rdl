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

    @Test
    void shouldBeConfigurable() {
        atomizer.configure(new PropertiesConfiguration())
    }

}
