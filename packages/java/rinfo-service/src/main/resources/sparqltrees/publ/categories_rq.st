PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX rpubl: <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#>


SELECT DISTINCT * WHERE {

    {
        ?item rdfs:label ?item__symbol;
            a ?item__type .
        ?item__instance a ?item;
            a ?item__siblingtype .
        ?item__siblingtype rdfs:subClassOf ?item__siblingtype__super1 .
        ?item__siblingtype__super1 rdfs:subClassOf ?item__siblingtype__super2 .
        OPTIONAL { ?item__siblingtype__super2 rdfs:subClassOf ?item__siblingtype__super3 }
        FILTER(
            ?item__siblingtype__super1 = rpubl:Rattsinformationsdokument ||
            ?item__siblingtype__super2 = rpubl:Rattsinformationsdokument ||
            ?item__siblingtype__super3 = rpubl:Rattsinformationsdokument
        )
    } UNION {
        ?item__instance dct:publisher ?item .
        OPTIONAL {
            ?item rdfs:label ?item__symbol .
        }
    } UNION {
        ?item__instance ?item ?item__symbol .
        FILTER(datatype(?item__symbol) = xsd:date)
    }

}
