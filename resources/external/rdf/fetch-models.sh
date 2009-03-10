#!/bin/bash

fetch-rdf() {
    url=$1
    file=$2
    if [[ ! -e $file ]]; then
        echo "Downloading <$url> as $file"
        curl -L -H "Accept: application/rdf+xml" -s $url > $file
    else
        echo "Skipping <$url> [Found file: $file]"
    fi
}

# W3C Standards
fetch-rdf 'http://www.w3.org/1999/02/22-rdf-syntax-ns#' rdf.rdfs
fetch-rdf 'http://www.w3.org/2000/01/rdf-schema#' rdf-schema.rdfs
fetch-rdf 'http://www.w3.org/2002/07/owl#' owl.owl
#fetch-rdf 'http://www.w3.org/2001/XMLSchema#' xsd.rdfs # TODO: 404

# W3C Draft Standards Data
# TODO: skos is expected to revert to the original ns fetched below,
# but currently, the latest model resides at: 'http://www.w3.org/2008/05/skos#'
fetch-rdf 'http://www.w3.org/2004/02/skos/core#' skos_core.rdfs

# Standard Community Data
fetch-rdf 'http://purl.org/dc/terms/' dcterms.rdfs
fetch-rdf 'http://purl.org/dc/elements/1.1/' dc_elements.rdfs

# De-facto Community Data
fetch-rdf 'http://xmlns.com/foaf/0.1/' foaf.owl
fetch-rdf 'http://rdfs.org/sioc/ns#' sioc.owl
fetch-rdf 'http://purl.org/ontology/bibo/' bibo.owl

# Experimenal Community Data
fetch-rdf 'http://bblfish.net/work/atom-owl/2006-06-06/#' atomowl.owl

# Legacy(?) Community Data
#fetch-rdf 'http://prismstandard.org/namespaces/1.2/basic/' prism.rdfs # TODO: 404

