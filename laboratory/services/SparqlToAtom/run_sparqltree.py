#!/usr/bin/env python
import os
from os.path import dirname, join
from lxml import etree
from urllib import urlencode
import httplib2


to_xslt = lambda fpath: etree.XSLT(etree.parse(fpath))
_modfpath = lambda fname: join(dirname(__file__), fname)

SPARQL_XSLT = to_xslt(_modfpath("tree-sparql.xslt"))
TRANSFORMER_XSLT = to_xslt(_modfpath("tree-transformer.xslt"))


class SparqlTree(object):

    def __init__(self, filename):
        self._filename = filename
        source_doc = etree.parse(filename)
        self._trans_doc = TRANSFORMER_XSLT(source_doc)
        self.sparql = str(SPARQL_XSLT(source_doc))
        self.transformer = etree.XSLT(self._trans_doc)

    def __call__(self, endpoint):
        return self.run_query(endpoint)

    def run_query(self, endpoint):
        rq_res = self._get_result(endpoint)
        return self.transformer(etree.fromstring(rq_res))

    def _get_result(self, endpoint):
        return query_sparql(self.sparql, endpoint)


def query_sparql(sparql, endpoint):
    h = httplib2.Http()
    data = {'query': sparql}
    headers = {
        'Accept': "application/sparql-results+xml",
        'Content-type': 'application/x-www-form-urlencoded'
    }
    resp, content = h.request(endpoint, "POST",
            body=urlencode(data),
            headers=headers)
    return content


compiler_cache = {}
def compiled(compiler, fname):
    prep = compiler_cache.get(fname)
    mtime = os.stat(fname).st_mtime
    if not prep or prep[0] < mtime:
        compiler_cache[fname] = mtime, compiler(fname)
        print "Compiled %s (last modified %s) with %s" % (
                fname, mtime, compiler)
    return compiler_cache[fname][1]


import cgi

class WSGIApp(object):

    def __init__(self, endpoint):
        self.endpoint = endpoint

    def __call__(self, environ, start_response):
        def start():
            start_response(status, headers.items())
        status = "200 OK"
        headers = {}

        path, query = self._parsed_env(environ)
        if path == '/':
            start()
            return self.render_index()

        filename = path[1:]
        rqtree = compiled(SparqlTree, filename)
        try:

            if "sparql" in query:
                headers['Content-type'] = "text/plain"
                body = str(rqtree.sparql)
            elif "transformer" in query:
                body = str(rqtree._trans_doc)
            elif "result" in query:
                rq_res = rqtree._get_result(self.endpoint)
                body = rq_res
            else:
                out = rqtree(self.endpoint)
                xsltname = query['apply'][0] if 'apply' in query else None
                if xsltname:
                    out = compiled(to_xslt, xsltname)(out)
                body = str(out)

        except Exception, e:
            status = "500 Server Error"
            body = str("Error: %s\n" % e.message +
                       "Using SPARQL:\n" +
                       "-"*40 +
                       "\n%s\n" % rqtree.sparql +
                       "-"*40
                )
            headers['Content-type'] = "text/plain"
        start()
        return body

    def render_index(self):
        import glob
        candidates = glob.glob("*.xml")
        yield """<html>
        <head><title>SPARQLTree</title></head>
        <body><ul>"""
        for fname in candidates:
            yield '<li><a href="%s">%s</a></li>' % (fname, fname)
        yield """ </ul></body></html>"""

    def _parsed_env(self, environ):
        path = environ['PATH_INFO']
        qstring = environ['QUERY_STRING']
        query = cgi.parse_qs(qstring)
        if not query:
            if '=' in qstring:
                query = dict([qstring.split('=')])
            else:
                query = {qstring: ""}
        return path, query


def wsgi_server(servername='', port=None, **kwargs):
    port = port or 8800
    from wsgiref.simple_server import make_server
    app = WSGIApp(**kwargs)
    httpd = make_server(servername, port, app)
    print "Serving HTTP on port %s..." % port
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == '__main__':

    from optparse import OptionParser

    parser = OptionParser(usage="%prog [options] [-h] <filename>")

    parser.add_option('-e', '--endpoint',
            help='Url to the sparql endpoint.')
    parser.add_option('-s', '--serve', type=int,
            help='Port to serve as web app.')

    parser.add_option('--sparql', action="store_true",
            help='Show the sparql extracted from the sparqltree')
    parser.add_option('--xslt', action="store_true",
            help='Show the xslt created from the sparqltree')

    parser.add_option('--result', action="store_true",
            help='Show raw sparql response xml')
    parser.add_option('--apply',
            help='Apply an xslt to the result tree')

    opts, args = parser.parse_args()
    if not opts.endpoint:
        parser.error("Please provide ENDPOINT.")

    if opts.serve:
        wsgi_server(port=opts.serve, endpoint=opts.endpoint)
    else:
        if not args:
            parser.error("Please provide one filename.")

        rqtree = SparqlTree(args[0])
        if opts.sparql:
            print rqtree.sparql
        elif opts.xslt:
            print rqtree._trans_doc
        elif opts.result:
            print rqtree._get_result(opts.endpoint)
        else:
            out = rqtree(opts.endpoint)
            if opts.apply:
                print to_xslt(opts.apply)(out)
            else:
                print out

