$sparqltrees/publ/base_rq(
    query_body={

        OPTIONAL {
            ?doc__1_publisher foaf:name ?doc__1_publisher__1_name .
        }

        OPTIONAL {
            ?doc rpubl:departement ?doc__1_departement .
            ?doc__1_departement foaf:name ?doc__1_departement__1_name .
        }

        OPTIONAL {
            ?doc rpubl:arsutgava ?doc__1_arsutgava .
            OPTIONAL { ?doc rpubl:lopnummer ?doc__1_lopnummer . }
            OPTIONAL { ?doc rpubl:sidnummer ?doc__1_sidnummer . }
            OPTIONAL { ?doc rpubl:bihangsnummer ?doc__1_bihangsnummer . }
        }

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

        } UNION {
            ?doc a rpubl:Proposition .

        } UNION {
            ?doc a rpubl:Utredningsbetankande;
                rpubl:utrSerie ?doc__1_utrSerie .

        #} UNION {
            # TODO: see uri-minter for remaining; e.g. (sync with base_rq.st):
            # Kommittedirektiv Forordningsmotiv
            # Rattelseblad
            # VagledandeMyndighetsavgorande + diarienummer
            # VagledandeDomstolsavgorande + malnummer + avgorandedatum
            # Rattsfallsreferat + referatserie
            # Rattsfallsnotis + notisserie
            # InternationellOverenskommelse
        }

        FILTER(?doc = <http://rinfo.lagrummet.se/publ/$path$>)

        # TODO: test these:
        OPTIONAL {
            ?doc rpubl:kapitel ?doc__kapitel .
                OPTIONAL {
                    ?doc__kapitel rpubl:kapitelnummer ?doc__kapitel__1_nr .
                }
            OPTIONAL {
                ?doc__kapitel rpubl:paragraf ?doc__kapitel__paragraf .
                OPTIONAL {
                    ?doc__kapitel__paragraf rpubl:paragrafnummer ?doc__kapitel__paragraf__1_nr .
                }
            }
        }
        OPTIONAL {
            ?doc rpubl:paragraf ?doc__paragraf .
            OPTIONAL {
                ?doc__paragraf rpubl:paragrafnummer ?doc__paragraf__1_nr .
            }
        }
        OPTIONAL {
            ?doc rpubl:bilaga ?doc__bilaga .
            OPTIONAL {
                ?doc__bilaga dct:title ?doc__bilaga__1_title .
            }
        }

        OPTIONAL {

            {
                # TODO: move outside of union (right?)
                # Date properties and values
                ?doc ?doc__daterel ?doc__daterel__1_value .
                FILTER(datatype(?doc__daterel__1_value) = xsd:date)
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

                ?doc__rev__subject ?doc__rev ?doc .
                FILTER(isURI(?doc__rev__subject))
                FILTER(?doc__rev != event:product)
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
