package se.lagrummet.rinfo.service.dataview

import se.lagrummet.rinfo.base.rdf.sparqltree.Lens
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens


class BasicViewHandler implements ViewHandler {

    Lens lens
    Map extraData

    BasicViewHandler(String locale, Map extraData=null) {
        this.lens = new SmartLens(locale)
        this.extraData = extraData
    }

    Map getQueryData() {
        return extraData
    }

    Map handleTree(Map tree) {
        return tree
    }

    Map handleGraph(Map graph) {
        if (extraData != null) {
            extraData.putAll(graph)
        }
        return graph
    }

}
