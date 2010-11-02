prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix owl: <http://www.w3.org/2002/07/owl#>
prefix skos: <http://www.w3.org/2004/02/skos/core#>
prefix dct: <http://purl.org/dc/terms/>
prefix foaf: <http://xmlns.com/foaf/0.1/>
prefix scv: <http://purl.org/NET/scovo#>
prefix tl: <http://purl.org/NET/c4dm/timeline.owl#>
prefix rpubl: <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#>
prefix rorg: <http://rinfo.lagrummet.se/org/>

select distinct
    ?docType
    ?docType__1_label
    ?docType__publisher
    ?docType__publisher__1_name
    ?docType__publisher__1_alt
    $if (publisher)$
        #?docType__publisher__stats
        #?docType__publisher__stats__1_count
        ?docType__publisher__time
        ?docType__publisher__time__1_prop
        ?docType__publisher__time__1_prop__1_label
        ?docType__publisher__time__1_year
    $endif$
where {


    ?_stats
        scv:dimension [ owl:onProperty rdf:type;
                owl:hasValue ?docType ];
        scv:dimension [ owl:onProperty dct:publisher;
                owl:hasValue ?docType__publisher ] .
        $if (publisher)$
            #?docType__publisher__stats
            #    rdf:value ?docType__publisher__stats__1_count;
            #    scv:dimension ?docType__publisher__stats__time .
            ?_stats scv:dimension ?docType__publisher__time .
            ?docType__publisher__time
                owl:onProperty ?docType__publisher__time__1_prop;
                tl:atYear ?docType__publisher__time__1_year .
            ?docType__publisher__time__1_prop
                rdfs:label ?docType__publisher__time__1_prop__1_label .
        $endif$

    ?docType rdfs:label ?docType__1_label .
    ?docType__publisher foaf:name ?docType__publisher__1_name .
    optional { ?docType__publisher skos:altLabel ?docType__publisher__1_alt }

    $if (docType)$
        FILTER(?docType = $docType$) .
    $endif$
    $if (publisher)$
        FILTER(?docType__publisher = rorg:$publisher$) .
        #FILTER( REGEX(STR(?docType__publisher), "[#/]$publisher$\$")) .
    $endif$

}

order by
    ?docType__label
    ?docType__publisher__1_name
    $if (publisher)$
        desc(?docType__publisher__time__1_year)
    $endif$

