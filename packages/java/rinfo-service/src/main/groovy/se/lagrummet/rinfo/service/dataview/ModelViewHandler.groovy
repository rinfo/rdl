package se.lagrummet.rinfo.service.dataview

import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder
import se.lagrummet.rinfo.base.rdf.sparqltree.Lens
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens


class ModelViewHandler extends BasicViewHandler {

    protected Map labels
    protected List allProperties

    ModelViewHandler(String locale, Map queryData, Map labelTree) {
        super(locale, queryData)
        this.labels = GraphBuilder.buildGraph(lens, labelTree)
    }

    Map handleTree(Map tree) {
        tree.remove('someProperty')
        return tree
    }

    Map handleGraph(Map graph) {
        this.allProperties = graph.ontology.collectAll { it.property }.flatten()
        def ontologies = graph.ontology.collect { prepOntology(it) }
        return [encoding: "utf-8",
                labels: labels,
                ontologies: ontologies]
    }

    def prepOntology(onto) {
        onto['sorted_classes'] = onto['class'].findAll {
                isDefinedBy(it, onto)
            }. collect {
                prepClass(it)
            }.sort(lsort)
        return onto
    }

    def prepClass(cls) {
        if (cls.ref_via.subClassOf) {
            cls['subclasses'] = cls.ref_via.subClassOf.sort(lsort)
        }
        cls['deprecated'] = cls.type.find {
                it.uri_term == 'DeprecatedClass'
            } != null
        def aggr = [:]
        mergeRestrictionsProperties(cls, aggr)
        cls['merged_restrictions_properties'] = aggr.values().sort(lsort)
        return cls
    }

    def prepProperty(prop) {
        if (prop.ref_via.subPropertyOf) {
            prop['subproperties'] = prop.ref_via.subPropertyOf.sort(lsort)
        }
        return prop
    }

    void mergeRestrictionsProperties(cls, aggr, direct=true) {
        def getSuper = true
        def add = {
            if (!aggr.containsKey(it.resource_uri)) {
                it['direct'] = direct
                aggr[it.resource_uri] = prepProperty(it)
            }
        }
        cls.restriction?.collect { propertyWithRestriction(it) }.each(add)

        allProperties.findAll {
            it.domain && it.domain.resource_uri == cls.resource_uri
        }.each(add)

        if (getSuper && cls.subClassOf) {
            for (superClass in cls.subClassOf) {
                mergeRestrictionsProperties(superClass, aggr, false)
            }
        }
    }

    def propertyWithRestriction(restr) {
        restr['cardinality_label'] = cardinalityLabel(
                labels.cardinalities, restr)
        restr['computed_range'] = computedRange(restr)
        def obj = new HashMap(restr.onProperty)
        obj.restriction=restr
        return obj
    }


    Closure lsort = {
        return it.label?.toLowerCase() ?: it.uri_term.toLowerCase()
    }

    boolean isDefinedBy(o, onto) {
        return (o.isDefinedBy?.resource_uri == onto.resource_uri)
    }

    String cardinalityLabel(labels, restr) {
        if (restr.cardinality == 0)
            return labels.zero_or_more // TODO: isn't this "not allowed"?
        else if (restr.cardinality == 1)
            return labels.exactly_one
        else if (restr.cardinality > 1)
            return "${labels.exactly} ${restr.cardinality}"
        else if (restr.minCardinality != null) {
            if (restr.minCardinality == 0 && !restr.maxCardinality)
                return labels.zero_or_more
            if (restr.minCardinality == 0 && restr.maxCardinality == 1)
                return labels.zero_or_one
            def l = null
            if (restr.minCardinality == 1)
                l = labels.at_least_one
            else if (restr.minCardinality)
                l = "${labels.at_least} ${restr.minCardinality}"
            if (l && restr.maxCardinality)
                l += ", ${labels.max} ${restr.maxCardinality}"
            if (l)
                return l
        }
        return labels.zero_or_more
    }

    def computedRange(restr) {
        def ranges = [restr.allValuesFrom, restr.someValuesFrom,
                restr.onProperty.range].findAll { it }
        def onto = restr.onProperty.isDefinedBy
        for (rg in ranges) {
            rg['same_ontology'] = onto != null && isDefinedBy(rg, onto)
        }
        return ranges
    }

}
