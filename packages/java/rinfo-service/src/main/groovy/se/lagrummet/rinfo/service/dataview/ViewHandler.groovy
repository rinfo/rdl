package se.lagrummet.rinfo.service.dataview

import se.lagrummet.rinfo.base.rdf.sparqltree.Lens


interface ViewHandler {

    Map getQueryData();
    Map handleTree(Map tree);
    Lens getLens();
    Map handleGraph(Map graph);

}
