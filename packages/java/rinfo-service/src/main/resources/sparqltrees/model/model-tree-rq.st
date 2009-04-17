PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX protege: <http://protege.stanford.edu/plugins/owl/protege#>

SELECT DISTINCT * WHERE {

    FILTER( ?ontology = <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#> ) .

    ?ontology a owl:Ontology .

    {
        OPTIONAL {
            ?ontology rdfs:label ?ontology__1_label
        }
        OPTIONAL {
            ?ontology rdfs:comment ?ontology__1_comment
        }

        OPTIONAL {
            ?ontology dct:title ?ontology__1_title
        }
        OPTIONAL {
            ?ontology dct:description ?ontology__1_description
        }

    } UNION {

        {
            ?ontology__class a owl:Class
        } UNION {
            ?ontology__class a owl:DeprecatedClass
        }
        ?ontology__class a ?ontology__class__type;
            rdfs:isDefinedBy ?ontology;
            rdfs:isDefinedBy ?ontology__class__1_isDefinedBy;
            rdfs:label ?ontology__class__1_label .

        OPTIONAL {
            ?ontology__class rdfs:comment ?ontology__class__1_comment
        }

        OPTIONAL {
            ?ontology__class protege:abstract ?ontology__class__1_abstract
        }

        OPTIONAL {
            ?ontology__class rdfs:subClassOf ?ontology__class__subClassOf .
            FILTER(isURI(?ontology__class__subClassOf))
        }

        OPTIONAL {
            ?ontology__class rdfs:subClassOf ?ontology__class__restriction .
            ?ontology__class__restriction a owl:Restriction;
                owl:onProperty ?ontology__class__restriction__1_onProperty .
            OPTIONAL {
                ?ontology__class__restriction owl:allValuesFrom ?ontology__class__restriction__1_allValuesFrom
            }
            OPTIONAL {
                ?ontology__class__restriction owl:someValuesFrom ?ontology__class__restriction__1_someValuesFrom
            }
            OPTIONAL {
                ?ontology__class__restriction owl:cardinality ?ontology__class__restriction__1_cardinality
            }
            OPTIONAL {
                ?ontology__class__restriction owl:minCardinality ?ontology__class__restriction__1_minCardinality
            }
            OPTIONAL {
                ?ontology__class__restriction owl:maxCardinality ?ontology__class__restriction__1_maxCardinality
            }
        }


    } UNION {

        {
            ?ontology__property a rdf:Property
        } UNION {
            ?ontology__property a owl:ObjectProperty
        } UNION {
            ?ontology__property a owl:DatatypeProperty
        } UNION {
            ?ontology__property a owl:FunctionalProperty
        }

        ?ontology__property a ?ontology__property__type;
            rdfs:isDefinedBy ?ontology;
            rdfs:isDefinedBy ?ontology__property__1_isDefinedBy;
            rdfs:label ?ontology__property__1_label
        OPTIONAL {
            ?ontology__property rdfs:comment ?ontology__property__1_comment
        }
        OPTIONAL {
            ?ontology__property rdfs:subPropertyOf ?ontology__property__subPropertyOf
        }
        OPTIONAL {
            ?ontology__property rdfs:domain ?ontology__property__1_domain
        }
        OPTIONAL {
            ?ontology__property rdfs:range ?ontology__property__1_range
        }
        OPTIONAL {
            ?ontology__property owl:inverseOf ?ontology__property__1_inverseOf .
            ?ontology__property__1_inverseOf rdfs:label ?ontology__property__1_inverseOf__1_label
        }
        OPTIONAL {
            ?ontology__property protege:abstract ?ontology__property__1_abstract
        }


    } UNION {

        ?someProperty rdfs:range ?ontology__class .
        ?ontology__class rdfs:isDefinedBy ?ontology__class__isDefinedBy;
            rdfs:label ?ontology__class__1_label .
        OPTIONAL {
            ?ontology__class rdfs:comment ?ontology__class__1_comment .
        }
        FILTER(?ontology__class__isDefinedBy != ?ontology)

    } UNION {

        ?ontology__property a rdf:Property;
            rdfs:isDefinedBy ?ontology__property__1_isDefinedBy;
            rdfs:label ?ontology__property__1_label .
        OPTIONAL {
            ?ontology__property rdfs:comment ?ontology__property__1_comment .
        }
        FILTER(?ontology__property__1_isDefinedBy != ?ontology)

    }

}