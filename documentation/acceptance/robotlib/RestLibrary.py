from urlparse import urljoin
import httplib2
from lxml import etree

class RestLibrary(object):

    def __init__(self):
        self._namespaces = {}
        self._status = None
        self._baseurl = None
        self._send_headers = {}
        self._data = None
        self._response = None
        self._content = None
        self._lazy_doc = None

    def base_url(self, url):
        self._baseurl = url

    def set_header(self, header, value):
        self._send_headers[header] = value

    def accept(self, mime_type):
        self.set_header('Accept', mime_type)

    def get(self, url):
        self._do_request("GET", url)

    def head(self, url):
        self._do_request("HEAD", url)

    def post(self, url, data):
        self._do_request("POST", url, data)

    def put(self, url, data):
        self._do_request("PUT", url, data)

    def delete(self, url):
        self._do_request("DELETE", url)

    def options(self, url):
        self._do_request("OPTIONS", url)

    def trace(self, url):
        self._do_request("TRACE", url)

    def patch(self, url, data):
        self._do_request("PATCH", url, data)

    def follow(self):
        cloc = self._response.get('content-location')
        assert cloc, (
                "Expected response header 'Content-Location' to be something but was nothing")
        self.get(cloc)

    def response(self, expected_status):
        self._expect("status", expected_status, self._status)

    def header(self, header, expected=None):
        value = self._response.get(header.lower())
        self._expect(header, expected, value)

    def xmlns(self, pfx, uri):
        self._namespaces[pfx] = uri

    def xpath(self, expr, expected):
        doc = self._get_parsed_doc()
        value = doc.xpath(expr, namespaces=self._namespaces)
        value = "".join(value)
        self._expect(expr, expected, value)

    def _do_request(self, method, url, data=None):
        h = httplib2.Http()
        url = urljoin(self._baseurl, url)
        response, content = h.request(url, method, data, headers=self._send_headers)
        self._status = "%d %s" % (response.status, response.reason)
        self._response = response
        self._content = content

    def _expect(self, key, expected, value):
        assert expected == value, (
                "Expected %s to be '%s' but was '%s'" % (key, expected, value))

    def _get_parsed_doc(self):
        if self._lazy_doc is None:
            self._lazy_doc = etree.fromstring(self._content)
        return self._lazy_doc

