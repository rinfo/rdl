# -*- coding: UTF-8 -*-
import simplejson
from sys import argv
from rdflib import ConjunctiveGraph as Graph
from rdflib import Namespace, RDF, RDFS, URIRef, BNode, Literal

FOAF = Namespace("http://xmlns.com/foaf/0.1/")

def rinfo_url(org):
    mailto = org.get('mailto', "")
    if not mailto.endswith("@dom.se"):
        #print "# Cannot generate URL for %r" % org
        return
    return "http://rinfo.lagrummet.se/org/%s" % mailto.split('@')[0]

f = argv[1]
orgs = simplejson.load(open(f))

graph = Graph()
graph.namespace_manager.bind('foaf', FOAF)

for org in orgs:
    url = rinfo_url(org)
    if not url:
        url = BNode()
    else:
        url = URIRef(url)
    def add(*args): graph.add(args)
    add(url, RDF.type, FOAF.Organization)
    add(url, FOAF.name, Literal(org['name'], lang='sv'))
    if 'url' in org:
        add(url, FOAF.homepage, URIRef(org['url']))
    if 'mailto' in org:
        add(url, FOAF.mbox, Literal(org['mailto']))
    add(url, RDFS.seeAlso, URIRef(org['via']))

print graph.serialize(format="n3")
