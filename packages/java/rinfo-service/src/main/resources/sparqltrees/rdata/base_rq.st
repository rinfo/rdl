PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX iana: <http://www.iana.org/assignments/relation/>
PREFIX awol: <http://bblfish.net/work/atom-owl/2006-06-06/#>
PREFIX tl: <http://purl.org/NET/c4dm/timeline.owl#>
PREFIX event: <http://purl.org/NET/c4dm/event.owl#>
PREFIX scv: <http://purl.org/NET/scovo#>
PREFIX rpubl: <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#>


SELECT DISTINCT * WHERE {

    ?doc a ?doc__1_type .

    ?doc__1_entry foaf:primaryTopic ?doc; a awol:Entry .

    {
        ?doc__1_entry awol:content ?doc__1_entry__repr .
    } UNION {
        ?doc__1_entry iana:alternate ?doc__1_entry__repr .
    }
    ?doc__1_entry__repr awol:type ?doc__1_entry__repr__1_mediaType .

    OPTIONAL { ?doc dct:title ?doc__1_title }

    OPTIONAL { ?doc__1_type rdfs:label ?doc__1_type__1_label }

    OPTIONAL { ?doc dct:publisher ?doc__1_publisher }

    ?doc dct:identifier ?doc__1_serieNummer . # TODO: adapt UI to this simpler form
    # TODO: enum of ?doc ?prop ?value where ?prop in [
    #    rpubl:arsutgava rpubl:lopnummer rpubl:sidnummer rpubl:bihangsnummer
    #]? or *all*?

    {
        ?doc rpubl:forfattningssamling ?doc__1_serie .
        ?doc__1_serie skos:altLabel ?doc__1_serie__1_label .

    } UNION {
        ?doc
            rpubl:utrSerie ?doc__1_serie;
        OPTIONAL { ?doc__1_serie skos:altLabel ?doc__1_serie__1_label }

    } UNION {
        ?doc # a rpubl:Rattsfallsreferat;
            rpubl:rattsfallspublikation ?doc__1_serie;
        OPTIONAL { ?doc__1_serie skos:altLabel ?doc__1_serie__1_label }
        OPTIONAL { ?doc rpubl:referatrubrik ?doc__1_title }
        # + from optional above: arsutgava, lopnummer
        #OPTIONAL { ?doc rpubl:notisserie ?doc__1_notisserie }
        #OPTIONAL { ?doc rpubl:referatserie ?doc__1_referatserie }

    } UNION {
        ?doc a rpubl:KonsolideradGrundforfattning;
          rpubl:konsoliderar [
            rpubl:forfattningssamling [
            # TODO: shortLabel..
              skos:altLabel ?serieLabel;
            ];
          ];
          #rpubl:utfardandedatum ?doc__1_konsUtfardandedatum .
          dct:issued ?doc__1_konsUtfardandedatum . # TODO: rename var

    #} UNION { # TODO: see uri-minter for remaining..
    }

    $if (path)$
        FILTER(?doc = <http://rinfo.lagrummet.se/publ/$path$>)
    $endif$


    $query_body$

}

$if (order_by)$ ORDER BY $order_by$ $endif$
$if (limit)$ LIMIT $limit$ $endif$

