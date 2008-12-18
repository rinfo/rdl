#!/bin/bash

python -m sparqltree.run -e http://localhost:8080/openrdf-sesame/repositories/rinfo?infer=false sparqltree-model.xml | xsltproc modeltree_to_html.xslt -

