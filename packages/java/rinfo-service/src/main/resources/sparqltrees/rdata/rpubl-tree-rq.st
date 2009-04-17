PREFIX st: <http://oort.to/ns/2008/09/sparqltree>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX rpubl: <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#>


SELECT DISTINCT * WHERE {

    # TODO: filter-var for doc (and use locale-var?)

    #FILTER(?doc = <http://rinfo.lagrummet.se/publ/sfs/1999:175>).

    ?doc a ?doc__1_type .

    OPTIONAL {
        ?doc__1_type rdfs:label ?doc__1_type__1_label .
    }

    OPTIONAL { ?doc dct:title ?doc__1_title }

    {
        ?doc
            rpubl:fsNummer ?doc__1_serieNummer;
            rpubl:forfattningssamling ?doc__1_serie .
        ?doc__1_serie rdfs:label ?doc__1_serie__1_label .

    } UNION {
        ?doc
            rpubl:utrSerie ?doc__1_serie;
            rpubl:utrSerienummer ?doc__1_serieNummer .
        OPTIONAL { ?serie rdfs:label ?doc__1_serie__label }

    # Rattsfallsreferat
    #} UNION {
    #

    } UNION {
        ?doc a rpubl:KonsolideradGrundforfattning;
          rpubl:konsoliderar [
            rpubl:forfattningssamling [
            # TODO: shortLabel..
              rdfs:label ?serieLabel;
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

    OPTIONAL {
        ?doc dct:publisher ?doc__1_publisher .
      OPTIONAL {
        ?doc__1_publisher rdfs:label ?doc__1_publisher__1_label .
      }
    }

    OPTIONAL {
      ?doc ?doc__daterel ?doc__daterel__value .
      FILTER(datatype(?doc__daterel__value) = xsd:date)
    }

    #$ if(get_relrev):{

        OPTIONAL {

            ?doc ?doc__rel ?doc__rel__subject .
                FILTER( isURI(?doc__rel__subject) )
                OPTIONAL {
                    ?doc__rel__subject a ?doc__rel__subject__1_type .
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
                REGEX(STR(?doc__rel__subject), "^http://rinfo\\.lagrummet\\.se/publ/")
            )

        }

##        # TODO: this one is *extremely* expensive..
##        OPTIONAL {
##        ?doc__rev__subject ?doc__rev ?doc .
##            OPTIONAL {
##            ?doc__rev owl:inverseOf ?doc__rev__1_inverse .
##            ?doc__rev__1_inverse rdfs:label ?doc__rev__1_inverse__1_label .
##            }
##        }
##
##    #}$

    #TODO:newline+ $filters$

}
