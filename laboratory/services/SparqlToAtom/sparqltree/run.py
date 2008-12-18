#!/usr/bin/env python
from sparqltree import SparqlTree
from sparqltree.webapp import wsgi_server

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

    import logging

    if opts.serve:
        logging.basicConfig(level=logging.INFO)
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

