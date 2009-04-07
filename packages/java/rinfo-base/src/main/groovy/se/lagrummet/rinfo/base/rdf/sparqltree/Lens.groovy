package se.lagrummet.rinfo.base.rdf.sparqltree


interface Lens {

    Map newResource(Map node);

    void mergeResources(Map source, Map result);

    void updateVia(resource, viaPair);

    Object castLiteral(Object node);

}
