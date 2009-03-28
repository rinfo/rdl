#!/usr/bin/env python
import os.path as p
from stringtemplate3 import StringTemplateGroup
from oort.sparqltree.run import run_query_to_tree, json
from modeldata import ModelData


def render_model(basedir, endpoint, locale):

    model_html = StringTemplateGroup("templates",
            basedir).getInstanceOf("model_html")
    query = open(p.join(basedir, "model-tree.rq")).read()

    from time import time
    start = time()
    tree = run_query_to_tree(endpoint, query)
    #print "# Query and tree done in %f s." % (time() - start)

    labeltree = json.load(open(p.join(basedir, "model_labels.json")))
    data = ModelData(locale, tree, labeltree)
    #print "# ModelData done in %f s." % (time() - start)

    model_html['locale'] = data.locale
    model_html['labels'] = data.labels
    model_html['ontologies'] = data.ontologies
    out = unicode(model_html).encode('utf-8')
    #print "# Template done in %f s." % (time() - start)
    return out


if __name__ == '__main__':

    viewdir = p.join(p.dirname(__file__),
            "../../../resources/sparqltrees/model/")
    print render_model(viewdir,
        "http://localhost:8080/openrdf-sesame/repositories/rinfo",
        "sv")

