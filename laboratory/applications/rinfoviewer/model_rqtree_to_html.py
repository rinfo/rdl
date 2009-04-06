#!/usr/bin/env python
import os.path as p
from stringtemplate3 import StringTemplateGroup
from oort.sparqltree.ext import json
from oort.sparqltree.access.util import discover_access

from modeldata import ModelData


def render_model(basedir, data_location, locale):

    query = open(p.join(basedir, "model-tree.rq")).read()

    from time import time
    start = time()
    access = discover_access(data_location)
    tree = access.run_query_to_tree(query)
    #print "# Query and tree done in %f s." % (time() - start)

    labeltree = json.load(open(p.join(basedir, "model_labels.json")))
    data = ModelData(locale, tree, labeltree)
    #print sum([len(c.merged_restrictions_properties)
    #        for o in data.ontologies for c in o.sorted_classes])
    #print "# ModelData done in %f s." % (time() - start)

    tplt = StringTemplateGroup("templates",
            basedir).getInstanceOf("model_html")
    tplt['encoding'] = 'utf-8'
    tplt['locale'] = data.locale
    tplt['labels'] = data.labels
    tplt['ontologies'] = data.ontologies
    out = unicode(tplt).encode('utf-8')
    #print "# Template done in %f s." % (time() - start)
    return out


if __name__ == '__main__':

    from sys import argv

    if len(argv) > 1:
        data_location = argv[1]
    else:
        #data_location = "http://localhost:8080/openrdf-sesame/repositories/rinfo"
        data_location = "http://localhost:2020/sparql"

    viewdir = p.join(p.dirname(__file__),
            "../../../resources/sparqltrees/model/")

    print render_model(viewdir, data_location, "sv")


