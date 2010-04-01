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

    ?doc__1_entry foaf:primaryTopic ?doc;
        a awol:Entry .
    {
        ?doc__1_entry awol:content ?doc__1_entry__repr .
    } UNION {
        ?doc__1_entry iana:alternate ?doc__1_entry__repr .
    }
    ?doc__1_entry__repr awol:type ?doc__1_entry__repr__1_mediaType .

    OPTIONAL { ?doc__1_type rdfs:label ?doc__1_type__1_label }

    OPTIONAL { ?doc dct:title ?doc__1_title }

    OPTIONAL {
        ?doc dct:publisher ?doc__1_publisher .
    }

    {
        ?doc
            rpubl:fsNummer ?doc__1_serieNummer;
            rpubl:forfattningssamling ?doc__1_serie .
        ?doc__1_serie skos:altLabel ?doc__1_serie__1_label .

    } UNION {
        ?doc
            rpubl:utrSerie ?doc__1_serie;
            rpubl:utrSerienummer ?doc__1_serieNummer .
        OPTIONAL { ?doc__1_serie skos:altLabel ?doc__1_serie__1_label }

    } UNION {
        ?doc # a rpubl:Rattsfallsreferat;
            rpubl:rattsfallspublikation ?doc__1_serie;
            rpubl:publikationsordinal ?doc__1_serieNummer .
        OPTIONAL { ?doc rpubl:referatrubrik ?doc__1_title }
        OPTIONAL { ?doc__1_serie skos:altLabel ?doc__1_serie__1_label }

    } UNION {
        ?doc a rpubl:KonsolideradGrundforfattning;
          rpubl:konsoliderar [
            rpubl:forfattningssamling [
            # TODO: shortLabel..
              skos:altLabel ?serieLabel;
            ];
            rpubl:fsNummer ?serieNummer
          ];
          rpubl:utfardandedatum ?doc__1_konsUtfardandedatum .

    # TODO: see uri-minter for remaining..
    } UNION {
            # TODO: shortLabel (of typeLabel)
        ?doc ?doc__1_rattsdokNrProperty ?doc__1_rattsdokNr .
        ?doc__1_rattsdokNrProperty rdfs:subPropertyOf rpubl:rattsdokumentnummer .
    }


    $if (path)$
        FILTER(?doc = <http://rinfo.lagrummet.se/publ/$path$>)
    $endif$


    $if (filter_parts)$
        ?doc ?doc__daterel ?doc__daterel__value .
        ?doc__daterel__eventItem
            event:product ?doc;
            scv:dimension ?doc__daterel;
            tl:atYear ?doc__daterel__eventItem__1_atYear .
        FILTER(
            $filter_parts:{
                $if (it.typeSelector)$
                    ?doc__1_type = $it.typeSelector$
                $elseif (it.dateSelector)$
                    (?doc__daterel = $it.dateSelector$ &&
                    STR(?doc__daterel__eventItem__1_atYear) = "$it.value$")
                $elseif (it.publisherSelector)$
                    REGEX(STR(?doc__1_publisher), "[#/]$it.value$\$")
                $endif$
            }; separator="&&"$
        )
    $endif$

    $if (details)$

        OPTIONAL {
            ?doc__1_publisher foaf:name ?doc__1_publisher__1_name .
        }

        OPTIONAL {

            {

                ?doc ?doc__daterel ?doc__daterel__value .
                FILTER(datatype(?doc__daterel__value) = xsd:date)
                ?doc__daterel rdfs:label ?doc__daterel__1_label .

            } UNION {

                ?doc ?doc__rel ?doc__rel__subject .
                    FILTER( isURI(?doc__rel__subject) )
                    OPTIONAL {
                        ?doc__rel__subject a ?doc__rel__subject__1_type .
                        ?doc__rel__subject__1_type
                            rdfs:label ?doc__rel__subject__1_type__1_label .
                    }

                OPTIONAL {
                    ?doc__rel__subject rdfs:label ?doc__rel__subject__1_label .
                }

                OPTIONAL {
                    ?doc__rel rdfs:label ?doc__rel__1_label .
                    OPTIONAL {
                    ?doc__rel rdfs:subPropertyOf ?doc__rel__1_superRel .
                    }
                }

                # TODO:
                # inclusive "?relType subClassOf Rattsinformationsdokument" instead?
                #   - no? must find doc via of related *parts* as well!
                #   - depends on if query is run against store or incoming subgraph.
                # Current, too loose variant:
                #FILTER( !BOUND(?superRel) ||
                #        ?superRel != dct:hasPart &&
                #        ?superRel != rpubl:publikationssamling )
                # Using *raw* "some kind of rinfo-doc" filter..:

                FILTER(
                    (!BOUND(?doc__rel__1_superRel) ||
                        ?doc__rel__1_superRel != dct:hasPart &&
                        ?doc__rel__1_superRel != rpubl:publikationssamling ) &&
                    REGEX(STR(?doc__rel__subject), "^http://rinfo\\\\.lagrummet\\\\.se/publ/")
                )

            } UNION {

                ?doc__rev__subject ?doc__rev ?doc . FILTER(isURI(?doc__rev__subject))
                OPTIONAL {
                    ?doc__rev__subject a ?doc__rev__subject__1_type .
                }
                FILTER(!(?doc__rev = foaf:primaryTopic &&
                        ?doc__rev__subject__1_type = awol:Entry))
                OPTIONAL {
                    ?doc__rev owl:inverseOf ?doc__rev__1_inverse .
                    ?doc__rev__1_inverse rdfs:label ?doc__rev__1_inverse__1_label .
                }

            }

        }

    $endif$

}

ORDER BY DESC(?doc)

$if (!filter_parts)$
    LIMIT $max_limit$
$endif$

