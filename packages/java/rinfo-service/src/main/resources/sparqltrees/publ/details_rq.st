$sparqltrees/publ/base_rq(
    query_body={

        OPTIONAL {
            ?doc__1_publisher foaf:name ?doc__1_publisher__1_name .
        }

        # TODO: use more stuff below in view..
        # .. + enum of ?doc ?prop ?value where ?prop in [
        #        rpubl:arsutgava rpubl:lopnummer rpubl:sidnummer rpubl:bihangsnummer
        #    ]? or all ?p ?o ?
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

        FILTER(?doc = <http://rinfo.lagrummet.se/publ/$path$>)

        OPTIONAL {

            {
                # Date properties and values
                ?doc ?doc__daterel ?doc__daterel__value .
                FILTER(datatype(?doc__daterel__value) = xsd:date)
                ?doc__daterel rdfs:label ?doc__daterel__1_label .

            } UNION {
                # Relations to other

                ?doc ?doc__rel ?doc__rel__subject .
                    FILTER( isURI(?doc__rel__subject) ) # no bnodes
                    OPTIONAL {
                        ?doc__rel__subject a ?doc__rel__subject__1_type .
                        ?doc__rel__subject__1_type
                            rdfs:label ?doc__rel__subject__1_type__1_label .
                    }
                OPTIONAL {
                    {
                        ?doc__rel__subject dct:identifier ?doc__rel__subject__1_repr .
                    } UNION {
                        ?doc__rel__subject rdfs:label ?doc__rel__subject__1_repr .
                    }
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
                # Other, incoming relations

                ?doc__rev__subject ?doc__rev ?doc . FILTER(isURI(?doc__rev__subject))
                OPTIONAL {
                    ?doc__rev__subject a ?doc__rev__subject__1_type .
                }
                OPTIONAL {
                    ?doc__rev__subject dct:identifier ?doc__rev__subject__1_repr .
                }
                FILTER(!(?doc__rev = foaf:primaryTopic &&
                        ?doc__rev__subject__1_type = awol:Entry))
                OPTIONAL {
                    ?doc__rev rdfs:label ?doc__rev__1_label .
                    OPTIONAL {
                        ?doc__rev owl:inverseOf ?doc__rev__1_inverse .
                        ?doc__rev__1_inverse rdfs:label ?doc__rev__1_inverse__1_label .
                    }
                }

            }

        }

    },
    limit=defaultSparqlLimit
)$
