# -*- coding: UTF-8 -*-
from lxml.etree import parse
from urllib import quote
from rdflib.graph import Graph
from rdflib.namespace import Namespace, RDFS
from rdfextras.tools.describer import Describer


SKOS = Namespace("http://www.w3.org/2004/02/skos/core#")
DCT = Namespace("http://purl.org/dc/terms/")
FOAF = Namespace("http://xmlns.com/foaf/0.1/")
RPUBL = Namespace("http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#")


translations = [
    (' ', '_'),
    ('å', 'aa'),
    ('ä', 'ae'),
    ('ö', 'oe'),
    ('Å', 'aa'),
    ('Ä', 'ae'),
    ('Ö', 'oe'),
]

def to_slug(text):
    text = text.lower()
    for char, repl in translations:
        text = text.replace(char, repl)
    return quote(text)


def fsdoc_to_graph(docpath):
    orgs, series, curr_org = [], [], None

    for tr in parse(docpath).xpath(
            u"//h:table[contains(h:tr/h:td, 'Benämning')]/h:tr",
            namespaces={'h':"http://www.w3.org/1999/xhtml"}):
        items = [ td.text.strip().encode('utf-8') for td in tr.findall('*') ]
        i = len(items)
        if i == 1:
            name = items[0]
            uri = '<http://rinfo.lagrummet.se/org/%s>' % to_slug(name)
            orgs.append((name, uri))
            curr_org = uri
        elif i >= 2:
            if i == 2:
                full, short = items
                comment = None
            else:
                full, short, comment = items
            uri = '<http://rinfo.lagrummet.se/serie/fs/%s>' % to_slug(short)
            series.append((uri, full, short, comment, curr_org))
        else:
            print "#", "|".join(items)

    g = Graph()
    g.bind('rdfs', RDFS)
    g.bind('dct', DCT)
    g.bind('skos', SKOS)
    g.bind('foaf', FOAF)
    g.bind('rpubl', RPUBL)
    d = Describer(g, base="http://rinfo.lagrummet.se/")

    for name, uri in orgs:
        d.about(uri[1:-1])
        d.rdftype(FOAF.Organization)
        d.value(FOAF.name, name, lang='sv')

    for uri, full, short, comment, org_uri in series:
        d.about(uri[1:-1])
        d.rdftype(RPUBL.Forfattningssamling)
        d.value(SKOS.altLabel, short, lang='sv')
        d.value(SKOS.prefLabel, full, lang='sv')
        if comment:
            d.value(RDFS.comment, comment, lang='sv')
        if org_uri:
            d.rel(DCT.publisher, org_uri[1:-1])

    return g


if __name__ == '__main__':
    import sys, os.path as p
    args = sys.argv[:]
    cmd = args.pop(0)

    if not args:
        print "USAGE: %s FILE" % p.basename(cmd)
        print "Where FILE is a local copy of <https://lagen.nu/1976:725>. Get it by doing e.g.:"
        print "  $ /usr/bin/curl -sk 'https://lagen.nu/1976:725' > /tmp/sfs-1976_725.xhtml"
        exit()
    docpath = args[0]

    graph = fsdoc_to_graph(docpath)

    from rdfextras.tools.pathutils import guess_format
    cmp_graph = Graph()
    for fpath in args[1:]:
        cmp_graph.load(fpath, format=guess_format(fpath))

    if cmp_graph:
        from rdflib.compare import graph_diff
        in_both, in_first, in_second = graph_diff(graph, cmp_graph)
        print "# %s new statements:" % len(in_first)
        for pfx, uri in graph.namespaces():
            in_first.bind(pfx, uri)
        print in_first.serialize(format='n3')

    else:
        print "# Nothing to compare against. New RDF is:"
        print graph.serialize(format='n3')

