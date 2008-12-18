# -*- coding: UTF-8 -*-
from os.path import dirname, join
from lxml import etree
from urllib import urlencode
import httplib2
import logging


_logger = logging.getLogger(__name__)


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
        _logger.info("Querying endpoint..")
        rq_res = self._get_result(endpoint)
        _logger.info("Transforming to tree..")
        return self.transformer(etree.fromstring(rq_res))
        _logger.info("Done.")

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


