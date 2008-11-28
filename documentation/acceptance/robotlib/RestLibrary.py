# -*- coding: UTF-8 -*-
"""
This is a HTTP/REST client library, primarily designed for use as a
`Robot Framework <http://robotframework.org/>`_ test library. It provides
keywords for calling REST-style services and inspecting the response.
"""

__author__ = "Niklas Lindström"
__version__ = "1.0a"


import re
from urlparse import urljoin
from warnings import warn

# Prefer httplib2, with fallback to (std) httplib.
try:
    from httplib2 import Http

    def http_request(url, method, data=None, headers=None):
        return Http().request(url, method, data, headers)

except ImportError:
    from urlparse import urlsplit
    from httplib import HTTPConnection, HTTPSConnection

    def http_request(url, method, data=None, headers=None):
        scheme, netloc, path, query, fragment = urlsplit(url)
        if scheme == 'https':
            conn = HTTPSConnection(netloc)
        else:
            conn = HTTPConnection(netloc)
        conn.request(method, "%s?%s" % (path, query), data, headers=headers)
        response = conn.getresponse()
        response.get = response.getheader # NOTE: monkey-patch
        return response, response.read()


# Assertion utilities.

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


# The library core.


class CoreRestClient(object):

    def __init__(self):
        self._status = None
        self._baseurl = None
        self._send_headers = {}
        self._reset()

    def _reset(self):
        self._response = None
        self._content = None

    def _do_request(self, method, url, data=None):
        self._reset()
        url = urljoin(self._baseurl, url)
        response, content = http_request(
                url, method, data, headers=self._send_headers)
        self._status = "%d %s" % (response.status, response.reason)
        self._response = response
        self._content = content

    def _get_header(self, header):
        return self._response.get(header.lower())


class CoreRestLibrary(CoreRestClient):

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

    def response(self, expected_status):
        expect("status", expected_status, self._status)

    def follow(self):
        header = 'Location'
        c_loc = self._get_header(header)
        # FIXME: should only use Location (and check for 30x), right?
        if not c_loc:
            header = 'Content-Location'
            c_loc = self._get_header(header)
        expect_exists("response header '%s'" % header, c_loc)
        self.get(c_loc)

    def header(self, header, expected=None):
        value = self._get_header(header)
        expect(header, expected, value)

    def body_is(self, expected):
        expect("response body", expected, self._content)

    def has_body(self):
        expect_exists("response body", self._content)

    def no_body(self):
        expect_not_exists("response body", self._content)


# Final library class (with ad hoc mixin support).

class RestLibrary(CoreRestLibrary):

    @classmethod
    def mixin(cls, klass):
        cls.__bases__ += (klass,)

    def __init__(self):
        self.__base_call('__init__')

    def _reset(self):
        self.__base_call('_reset')

    def __base_call(self, name, *args, **kwargs):
        for base in type(self).__bases__:
            if hasattr(base, name):
                getattr(base, name)(self, *args, **kwargs)


# Additional feature mixins.


class XmlMixinSupport(object):

    def __init__(self):
        self._namespaces = {}

    def _reset(self):
        self._lazy_xml = None

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
        value = self._eval_xpath(expr)
        expect_exists("xpath %r" % expr, value)
        return value

    def no_xpath(self, expr):
        value = self._eval_xpath(expr)
        expect_not_exists("xpath %r" % expr, value)

    def _get_parsed_xml(self):
        raise NotImplementedError("No XML parser available.")

    def _eval_xpath(self, expr):
        raise NotImplementedError("No XPath processor available.")

try:
    from lxml import etree

    class XmlMixin(XmlMixinSupport):

        def _get_parsed_xml(self):
            if self._lazy_xml is None:
                self._lazy_xml = etree.fromstring(self._content)
            return self._lazy_xml

        def _eval_xpath(self, expr):
            doc = self._get_parsed_xml()
            return doc.xpath(expr, namespaces=self._namespaces)

    RestLibrary.mixin(XmlMixin)

except ImportError:
    warn("Cannot parse XML responses. Missing module: lxml")
    # TODO: Try some more options (4Suite, elementtree, javax.xml).
    # If none can be found, disable XML keywords by removing the following line:
    RestLibrary.mixin(XmlMixinSupport)


# TODO: No features yet.
class JsonMixin(object):
    def _reset(self):
        self._lazy_json = None
RestLibrary.mixin(JsonMixin)


