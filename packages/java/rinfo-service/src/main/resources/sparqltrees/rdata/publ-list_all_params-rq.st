prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix owl: <http://www.w3.org/2002/07/owl#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>
prefix skos: <http://www.w3.org/2004/02/skos/core#>
prefix foaf: <http://xmlns.com/foaf/0.1/>
prefix scv: <http://purl.org/NET/scovo#>
prefix event: <http://purl.org/NET/c4dm/event.owl#>
prefix tl: <http://purl.org/NET/c4dm/timeline.owl#>
prefix rpubl: <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#>
prefix dct: <http://purl.org/dc/terms/>

select distinct
    ?docType
    ?docType__label
    ?docType__publisher
    ?docType__publisher__1_name
    ?docType__publisher__1_alt
    ?docType__publisher__eventItem
    ?docType__publisher__eventItem__1_year
    ?docType__publisher__eventItem__1_property
where {

    $if (publisher)$

        ?docType__publisher__eventItem
            tl:atYear ?docType__publisher__eventItem__1_year;
            scv:dimension ?docType__publisher__eventItem__1_property .
        {
            ?docType__publisher__eventItem__1_property rdfs:domain ?docType .
        } union {
            ?docType rdfs:subClassOf [
                owl:onProperty ?docType__publisher__eventItem__1_property
            ] .
        }

        ?docType__publisher__eventItem
    $else$
        []
    $endif$

    event:product [
        a ?docType;
        dct:publisher ?docType__publisher
    ] .
    ?docType rdfs:label ?docType__label .
    ?docType__publisher foaf:name ?docType__publisher__1_name .
    optional { ?docType__publisher skos:altLabel ?docType__publisher__1_alt }

    $if (docType)$
        FILTER(?docType = $docType$) .
    $endif$
    $if (publisher)$
        #FILTER(?docType__publisher = $publisher$) .
        FILTER(
            REGEX(STR(?docType__publisher), "[#/]$publisher$\$")
        ) .
    $endif$

}

order by
    ?docType__label
    ?docType__publisher__1_name
    desc(?docType__publisher__eventItem__1_year)

