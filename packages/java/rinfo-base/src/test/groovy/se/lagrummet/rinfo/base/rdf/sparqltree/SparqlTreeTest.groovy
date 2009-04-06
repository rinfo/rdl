package se.lagrummet.rinfo.base.rdf.sparqltree

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*


class SparqlTreeTest {

    SparqlTree rqTree

    @Before
    void setup() {
    }

    @Test
    void shouldCreateVarModel() {
        def vars = ["org", "org__1_name", "org__feed", "org__feed__1_title"]
        /* ...
        def model = SparqlTree.makeVarTreeModel
        def expected = "{org:[false, org, {name:[true, org__1_name, {}]}, {feed:[false, org__feed, {title:[true, org__feed__1_title, {}]}]}]}"


        assertEquals "", model.toString()
        */
    }

    @Test
    void shouldBuildTree() {
        // ...
    }

}
