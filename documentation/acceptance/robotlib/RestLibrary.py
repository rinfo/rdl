import re
from urlparse import urljoin
import httplib2
from lxml import etree


class RestLibrary(object):

    def __init__(self):
        self._namespaces = {}
        self._status = None
        self._baseurl = None
        self._send_headers = {}
        self._reset()

    def _reset(self):
        self._response = None
        self._content = None
        self._lazy_doc = None

    def base_url(self, url):
        self._baseurl = url

    def set_header(self, header, value):
        self._send_headers[header] = value

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
        c_loc = self._response.get('content-location')
        expect_exists("response header 'Content-Location'", c_loc)
        self.get(c_loc)

    def response(self, expected_status):
        expect("status", expected_status, self._status)

    def header(self, header, expected=None):
        value = self._response.get(header.lower())
        expect(header, expected, value)

    def body_is(self, expected):
        expect("response body", expected, self._content)

    def has_body(self):
        expect_exists("response body", self._content)

    def no_body(self):
        expect_not_exists("response body", self._content)

    def xmlns(self, pfx, uri):
        self._namespaces[pfx] = uri

    def xpath_value(self, expr, expected):
        value = self.find_xpath(expr)
        value = "".join(value)
        expect("xpath %r" % expr, expected, value)
        return value

    def xpath_regexp(self, expr, regexp):
        value = self.find_xpath(expr)
        value = "".join(value)
        expect_regexp("xpath %r" % expr, regexp, value)
        return value

    def find_xpath(self, expr):
        doc = self._get_parsed_doc()
        value = doc.xpath(expr, namespaces=self._namespaces)
        expect_exists("xpath %r" % expr, value)
        return value

    def no_xpath(self, expr):
        doc = self._get_parsed_doc()
        value = doc.xpath(expr, namespaces=self._namespaces)
        expect_not_exists("xpath %r" % expr, value)

    def _do_request(self, method, url, data=None):
        self._reset()
        h = httplib2.Http()
        url = urljoin(self._baseurl, url)
        response, content = h.request(url, method, data, headers=self._send_headers)
        self._status = "%d %s" % (response.status, response.reason)
        self._response = response
        self._content = content

    def _get_parsed_doc(self):
        if self._lazy_doc is None:
            self._lazy_doc = etree.fromstring(self._content)
        return self._lazy_doc


def expect(key, expected, value):
    assert expected == value, (
            "Expected %s to be %r but was %r" % (key, expected, value))

def expect_regexp(key, regexp, value):
    assert re.match(regexp, value), (
            "Expected %s to match regexp %r but was %r" % (key, regexp, value))

def expect_exists(what, value):
    assert value, "Expected %s to be present (got %r)." % (what, value)

def expect_not_exists(what, value):
    assert not value, "Expected no value for %s (got %r)." % (what, value)


