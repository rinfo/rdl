from __future__ import with_statement
import urllib2
import os
import time
import shutil
import functools as f


projectroot = os.path.join(os.path.dirname(__file__), '../../')


def download_models(usemodtime=True):
    destdir = projectroot+"resources/external/rdf/"
    get = f.partial(_http_get, usemodtime=_unrepr(usemodtime),
             headers=[('Accept', "application/rdf+xml")])

    # W3C Standards
    get("http://www.w3.org/1999/02/22-rdf-syntax-ns#", destdir+"rdf.rdfs")
    get("http://www.w3.org/2000/01/rdf-schema#", destdir+"rdf-schema.rdfs")
    get("http://www.w3.org/2002/07/owl#", destdir+"owl.owl")
    #get("http://www.w3.org/2001/XMLSchema#", destdir+"xsd.rdfs") # 404:s..

    # W3C Draft Standards Data
    get("http://www.w3.org/2004/02/skos/core#", destdir+"skos_core.rdfs")

    # Standard Community Data
    get("http://purl.org/dc/terms/", destdir+"dcterms.rdfs")
    get("http://purl.org/dc/elements/1.1/", destdir+"dc_elements.rdfs")

    # De-facto Community Data
    get("http://xmlns.com/foaf/0.1/", destdir+"foaf.owl")
    get("http://rdfs.org/sioc/ns#", destdir+"sioc.owl")
    get("http://purl.org/ontology/bibo/", destdir+"bibo.owl")

    # Experimental Community Data
    get("http://bblfish.net/work/atom-owl/2006-06-06/#", destdir+"atomowl.owl")

    # Legacy(?) Community Data
    #get("http://prismstandard.org/namespaces/1.2/basic/",
    #        destdir+"prism.rdfs") # 404:s..

def download_xslt(usemodtime=True):
    destdir = projectroot+"resources/external/xslt/"
    get = f.partial(_http_get, usemodtime=_unrepr(usemodtime))
    get("http://purl.org/oort/impl/xslt/tram/rdfxml-tram.xslt", destdir)

def download_all(usemodtime=True):
    download_models(usemodtime)
    download_xslt(usemodtime)


_unrepr = lambda v: eval(v) if isinstance(v, str) else v


def _http_get(url, dest, usemodtime=True, headers=()):
    if dest.endswith('/'):
        dest += url.rsplit('/', 1)[-1]
    print "Download <%(url)s> to <%(dest)s>"%vars()
    req = urllib2.Request(url)
    for header, value in headers:
        req.add_header(header, value)
    if os.path.exists(dest):
        if not usemodtime:
            print "Destination exists, skipping."
            return
        modstamp = time.strftime("%a, %d %b %Y %H:%M:%S GMT",
                time.gmtime(os.stat(dest).st_mtime))
        req.add_header('If-Modified-Since', modstamp)
    try:
        res = urllib2.urlopen(req)
        print res.info()
    except Exception, e:
        print e
        return
    with file(dest, 'w') as out:
        shutil.copyfileobj(res, out)

