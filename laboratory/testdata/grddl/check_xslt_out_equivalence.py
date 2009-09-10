#!/usr/bin/env python
from __future__ import with_statement
from rdflib.graphutils import IsomorphicGraph
import os
from sys import argv

testdir = argv[1]
source = argv[2]

with os.popen("./process.sh %(source)s %(testdir)s/html_to_rdfa.xslt"%vars()) as pipe1:
    g1 = IsomorphicGraph()
    g1.parse(pipe1, format="rdfa")

with os.popen("./process.sh %(source)s %(testdir)s/html_to_rdfxml.xslt"%vars()) as pipe2:
    g2 = IsomorphicGraph()
    g2.parse(pipe2, format="xml")

l1, l2 = len(g1), len(g2)
print "Sizes:", l1, l2,
equal = g1 == g2
print "Isomorphic:", equal
if not equal:
    for t in g1:
        if not t in g2:
            print "Missing triple in second graph:"
            print " "*4,
            for node in t:
                print node.n3().encode('utf-8'),
            print "."
            #s1, p1, o1 = t
            #o2 = g2.value(s1, p1)
            #print "Object in g1: %r" % o1,
            #print "Object in g2: %r" % o2

