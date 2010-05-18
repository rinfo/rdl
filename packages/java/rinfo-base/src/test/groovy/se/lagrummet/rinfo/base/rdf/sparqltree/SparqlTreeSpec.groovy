package se.lagrummet.rinfo.base.rdf.sparqltree


import spock.lang.*


class SparqlTreeSpec extends Specification {

    SparqlTree rqTree

    void setup() {
    }

    def "should create var model"() {
        def vars = ["org", "org__1_name", "org__feed", "org__feed__1_title"]
        /* ...
        def model = SparqlTree.makeVarTreeModel
        def expected = "{org:[false, org, {name:[true, org__1_name, {}]}, {feed:[false, org__feed, {title:[true, org__feed__1_title, {}]}]}]}"


        assertEquals "", model.toString()
        */
    }

    def "should build tree"() {
        // ...
    }

}
