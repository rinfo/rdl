#!/bin/bash

python -m sparqltree.run -e http://localhost:8080/openrdf-sesame/repositories/rinfo?infer=false examples/model/sparqltree-model.xml | xsltproc examples/model/modeltree_to_html.xslt -

