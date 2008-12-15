# -*- coding: UTF-8 -*-
import cgi
from sparqltree import SparqlTree


compiler_cache = {}
def compiled(compiler, fname):
    prep = compiler_cache.get(fname)
    mtime = os.stat(fname).st_mtime
    if not prep or prep[0] < mtime:
        compiler_cache[fname] = mtime, compiler(fname)
        print "Compiled %s (last modified %s) with %s" % (
                fname, mtime, compiler)
    return compiler_cache[fname][1]


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


