package se.lagrummet.rinfo.service.dataview

import se.lagrummet.rinfo.base.rdf.sparqltree.Lens
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree


class RDataViewHandler implements ViewHandler {

    Lens lens
    String locale
    Map queryData
    Map appData

    RDataViewHandler(String locale, Map appData, Map queryData) {
        this.locale = locale
        this.appData = appData
        this.queryData = queryData
        this.lens = new RDataLens(locale)
    }

    Map handleTree(Map tree) {
        return tree
    }

    Map handleGraph(Map graph) {
        if (queryData != null) graph.putAll(appData)
        if (appData != null) graph.putAll(queryData)
        return graph
    }

    // TODO:
    // - match certain resources/types?
    // - complements resource_uri with an "rdata_url" (+ "service urls" 4 facets)
    // - composes shortLabel, fullLabel, ...
    // - load the atom-entry for publ-resources, unless stored awol:Entry in repo

}

class RDataLens extends SmartLens {
    RDataLens(String locale) {
        super(locale)
    }

    Map newResource(Map node) {
        def rnode = super.newResource(node)
        def uri = rnode['resource_uri']
        if (uri) {
            rnode['rdata_url'] = rdataUrl(uri)
        }
        return rnode
    }

    Object castLiteral(Object node) {
        def value
        if (SparqlTree.isDatatypeNode(node)) {
            value = node[SparqlTree.VALUE_KEY]
        } else {
            value = super.castLiteral(node)
        }
        // TODO: html-escape text (unless xmlliterals)
        return value
    }

    def rdataUrl(uri) {
        return uri.replace("http://rinfo.lagrummet.se/publ/",
                "/rdata/publ/")
    }

}

