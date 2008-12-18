#!/bin/bash

python -m sparqltree.run -e http://localhost:8080/openrdf-sesame/repositories/rinfo?infer=false sparqltree-rdata_entry.xml | xsltproc tree_to_atom.xslt - | xmllint --format -

