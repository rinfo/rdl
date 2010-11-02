package se.lagrummet.rinfo.service

import org.openrdf.repository.RepositoryConnection

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description
import se.lagrummet.rinfo.base.rdf.RDFLiteral


class EntryStats {

    RepositoryConnection conn

    def statsContext = "tag:lagrummet.se,2010:stats#context"
    def statsItemBaseUri = "tag:lagrummet.se,2010:stats/item/"
    def dimensionBaseUri = "tag:lagrummet.se,2010:stats/dim/"

    def additionalMeasurements = ["http://purl.org/dc/terms/publisher",
        "http://purl.org/dc/terms/creator"]

    Description doc
    Describer desc

    EntryStats(RepositoryConnection conn, String docUri, String docContext) {
        this.conn = conn
        this.doc = newDocDescriber(docContext).newDescription(docUri)
        this.desc = newStatsDescriber()
    }

    def addStatistics() {
        def dimLocalUris = dimensionsFor(doc)
        if (dimLocalUris.size() == 0)
            return null
        def statsItemUri = createStatsItemUri(dimLocalUris)
        def statsItem = desc.findDescription(statsItemUri)
        if (statsItem == null) {
            statsItem = desc.newDescription(statsItemUri, "scv:Item")
        }
        for (dimLocalUri in dimLocalUris) {
            statsItem.addRel("scv:dimension", dimensionBaseUri + dimLocalUri)
        }
        Integer count = statsItem.getNative("rdf:value")
        if (count == null) count = 0
        statsItem.remove("rdf:value")
        statsItem.addLiteral("rdf:value", count + 1)
        statsItem.addRel("event:product", doc.about)
        return statsItem
    }

    def removeStatistics() {
        // FIXME: implement!
        //def statsItems = desc.subjects("event:product", doc.about)
        //for (statsItem in statsItems) {
        //    remove product
        //    decrease value by 1
        //    if (value == 0) {
        //        remove dimensions if only ref:ed from statsItem(s) to be removed
        //        remove statsItem
        //    }
        //}
    }

    def createStatsItemUri(SortedSet<String> dimLocalUris) {
        return statsItemBaseUri + dimLocalUris.join("/")
    }

    def dimensionsFor(Description doc) {
        def dimLocalUris = new TreeSet<String>()
        for (triple in doc.getTriples()) {
            def dimLocalUri = createDimensionLocalUri(triple.property, triple.object)
            if (dimLocalUri != null) {
                def dimUri = dimensionBaseUri + dimLocalUri
                ensureDimension(dimUri, triple.property, triple.object)
                dimLocalUris.add(dimLocalUri)
            }
        }
        return dimLocalUris
    }

    def ensureDimension(dimUri, property, value) {
        def dimension = desc.findDescription(dimUri)
        if (dimension == null) {
            dimension = desc.newDescription(dimUri, "scv:Dimension")
        }
        dimension.addRel("owl:onProperty", property)
        def year = tryGetYear(value)
        if (year != null) {
            dimension.addLiteral("tl:atYear", year, "xsd:gYear")
        } else if (value instanceof String) {
            dimension.addRel("owl:hasValue", value)
        }
        return dimension
    }

    def createDimensionLocalUri(property, value) {
        def propCurie = desc.toCurie(property)
        if (propCurie == null) {
            return null
        }
        def year = tryGetYear(value)
        if (year != null) {
            return propCurie + "@" + year
        } else if (value instanceof String) {
            if (!property.equals(Describer.RDF_NS + "type") &&
                !additionalMeasurements.contains(property)) {
                return null
            }
            def valueCurie = desc.toCurie(value)
            if (valueCurie != null) {
                return propCurie + "+" + valueCurie
            }
        } else {
            return null
        }
    }

    String tryGetYear(Object value) {
        if (value instanceof RDFLiteral) {
            if (value.isGCalType()) {
                return value.toXmlGCal().getEonAndYear().toString()
            }
        }
        return null
    }

    def newDocDescriber(String... contexts) {
        return new Describer(conn, false, contexts).
            setPrefix("dct", "http://purl.org/dc/terms/").
            setPrefix("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#").
            setPrefix("iana", "http://www.iana.org/assignments/relation/").
            setPrefix("foaf", "http://xmlns.com/foaf/0.1/")
    }

    def newStatsDescriber() {
        return new Describer(conn, false, statsContext).
            setPrefix("dct", "http://purl.org/dc/terms/").
            setPrefix("scv", "http://purl.org/NET/scovo#").
            setPrefix("event", "http://purl.org/NET/c4dm/event.owl#").
            setPrefix("tl", "http://purl.org/NET/c4dm/timeline.owl#").
            setPrefix("rpubl", "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#").
            setPrefix("rorg", "http://rinfo.lagrummet.se/org/")
    }

    /*
    class Dimension {
        boolean timeDimension
        String property
        String valueRepr
        Dimension() {
        }
    }
    */

}
