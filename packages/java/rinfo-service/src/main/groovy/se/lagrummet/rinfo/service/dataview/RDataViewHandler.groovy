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

    String baseUrl

    RDataLens(String locale) {
        super(locale)
        baseUrl = "http://rinfo.lagrummet.se/"
    }

    Map newResource(Map node) {
        def rnode = super.newResource(node)
        decorateResource(rnode)
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

    void decorateResource(Map rnode) {
        def uri = rnode['resource_uri']
        if (!uri || !uri.startsWith(baseUrl))
            return
        def path = uri.substring(baseUrl.size())
        rnode['rdata_url'] = "/rdata/" + path
        //switch (path) {
        //    case ~/publ\/.+/:
        //    break
        //}
    }

}

