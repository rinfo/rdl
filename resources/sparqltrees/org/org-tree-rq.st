prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix skos: <http://www.w3.org/2004/02/skos/core#>
prefix dct: <http://purl.org/dc/terms/>
prefix bibo: <http://purl.org/ontology/bibo/>
prefix foaf: <http://xmlns.com/foaf/0.1/>
prefix awol: <http://bblfish.net/work/atom-owl/2006-06-06/#>
prefix iana: <http://www.iana.org/assignments/relation/>

select distinct * where {
    ?org a foaf:Organization;
        foaf:name ?org__1_name .
    optional { ?org skos:altLabel ?org__1_alt }
    optional { ?org rdfs:comment ?org__1_comment }
    optional {
        ?org__serie
            dct:publisher ?org;
            a ?org__serie__1_type .
        {
            ?org__serie__1_type rdfs:subClassOf bibo:Collection
        } union {
            ?org__serie__1_type rdfs:subClassOf [
                rdfs:subClassOf bibo:Collection
            ]
        }
        ?org__serie__1_type rdfs:label ?org__serie__1_type__1_label .
        {
            ?org__serie skos:prefLabel ?org__serie__1_someLabel
        } union {
            ?org__serie rdfs:label ?org__serie__1_someLabel
        }
        optional { ?org__serie skos:altLabel ?org__serie__1_shortname }
        optional { ?org__serie rdfs:comment ?org__serie__1_comment }
    }
    optional {
        ?org__feed
            a awol:Feed;
            awol:id ?org__feed__1_id;
            dct:publisher ?org;
            iana:current ?org__feed__1_current .
    }
}
order by desc(?org)
