########################################################################
RInfo Core REST Application Specification
########################################################################

.. Note! This document is an executable test. Run with::
    $ python -m resttest --baseurl http://rinfo.lagrummet.se <document>


Introduction
========================================================================

Following the specs and guidelines in:

Best Practice Recipes for Publishing RDF Vocabularies
    <http://www.w3.org/TR/swbp-vocab-pub/>

Cool URIs for the Semantic Web
    <http://www.w3.org/TR/cooluris/>

On Linking Alternative Representations To Enable Discovery And Publishing
    <http://www.w3.org/2001/tag/doc/alternatives-discovery.html>

HTTP/1.1 Status Code Definitions:
    <http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html>


Core Resource Application
========================================================================

In these examples, the following namespaces are used::

    >>> xmlns(h="http://www.w3.org/1999/xhtml")
    >>> xmlns(atom="http://www.w3.org/2005/Atom")


Core Feeds
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

.. TODO: should feeds be localized (both ideally and specifically since they
   cannot contain e.g. duplicate atom:title)?

The root of the resource application contains a couple of feeds::

    >>> http_get("/feed",
    ...         xpath("/atom:feed/atom:link[@rel='next-archive']"))
    ...
    200 OK
    Content-Type: application/atom+xml;feed
    [XPath 1 matched]


Core Service Metadata
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

An extended sitemap.xml is available to provide locations of SPARQL-endpoints 
and eventual data dumps (see `Semantic Web Crawling: A Sitemap Extension 
<http://sw.deri.org/2007/07/sitemapextension/>`_).

Namespaces used::

    >>> xmlns(sm="http://www.sitemaps.org/schemas/sitemap/0.9")
    >>> xmlns(sc="http://sw.deri.org/2007/07/sitemapextension/scschema.xsd")

..  TODO: define what is pointed to in index.xhtml

Discovering information from the index page::

    >>> http_get("/",
    ...         xpath("/h:html/h:body/h:link[@rel='']"))
    ...
    200 OK
    Content-Type: application/xhtml+xml
    [XPath 1 matched]

.. TODO: define exactly what to expose

The sitemap extensions are::

    >>> http_get("/sitemap.xml",
    ...         xpath("sm:urlset/sc:dataset/sc:sparqlEndpointLocation"))
    ...
    200 OK
    Content-Type: text/xml
    [XPath 1 matched]


..  TODO: extrapolate sketch::

    Atom-feed med alla resurser: scheman, "listor" och dokument:

    <http://rinfo.lagrummet.se/feed>
        .. @rel="archive-prev"
        .. Fråga: @rel="next" som inte visar gamla "updateds" och inga deleteds?
            .. grymt bra för första-laddningen!
            .. dock archives med tombstones och tidigare updates;
            ska vara med i beräkningen för laddning, då gamla poster
            man läst in måste delete:as! Tidigare updates inte lika viktigt
            för inläsning? Dock extremt så för historiken!

        Rimligt även med feed per collection:
        <http://rinfo.lagrummet.se/publ/> =>
            <http://rinfo.lagrummet.se/publ/feed>

            <http://rinfo.lagrummet.se/publ/container>
                -> SIOC-container metadata (det som ingår i uri-strategin)

    Segmenten under en collection är delar av de ogenomskinliga identifierarna 
    för respektive entry. Så går man på "en halv" ges 404 (att t.ex. studsa upp 
    till collection:en vore märkligt för ett felaktigt path-värde..).

    > GET http://rinfo.lagrummet.se/publ/sfs/
    404 Not Found


Model Retrieval
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++



Content Negotiation for Entries
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

.. TODO: extrapolate sketch::
    <http://rinfo.lagrummet.se/publ/sfs/1999:175> =>
        - <http://rinfo.lagrummet.se/publ/sfs/1999:175/pdf> =>
            <http://rinfo.lagrummet.se/publ/sfs/1999:175/pdf,sv>
        - <http://rinfo.lagrummet.se/publ/sfs/1999:175/xhtml> =>
            <http://rinfo.lagrummet.se/publ/sfs/1999:175/xhtml,sv>
        - <http://rinfo.lagrummet.se/publ/sfs/1999:175/entry>

Retrieving a document entry with content-negotiation::

    >>> set_header("Accept", "application/pdf")
    >>> http_get("/publ/sfs/1999:175")
    ...
    200 OK
    Content-Type: application/pdf
    Content-Language: sv
    Content-Location: /publ/sfs/1999:175/pdf,sv
    Vary: Accept-Charset, Accept-Encoding, Accept-Language, Accept
    [Body]

Getting the RDF::

    >>> http_get("/publ/sfs/1999:175",
    ...         send_headers={"Accept": "application/rdf+xml"})
    ...
    303 See Other
    Location: /publ/sfs/1999:175/rdf
    >>> follow()
    [Following..]
    200 OK
    Content-Type: application/rdf+xml
    [Body]

Meta-data only::

    >>> http_head("/publ/sfs/1999:175/pdf,sv")
    200 OK
    Content-Type: application/pdf

TODO: When do we want:
    300 Multiple Choices

Retrieval with cache control::

    >>> http_get("/publ/sfs/1999:175/pdf,sv", silent=True)
    >>> http_get("/publ/sfs/1999:175/pdf,sv",
    ...         send_headers={"If-None-Match": prev_header("ETag")})
    304 Not Modified

    >>> http_get("/publ/sfs/1999:175/pdf,sv", silent=True)
    >>> set_header("If-Modified-Since", prev_header("Last-Modified"))
    >>> http_get("/publ/sfs/1999:175/pdf,sv")
    304 Not Modified

Missing documents:

    >>> http_get("/publ/sfs/-1")
    404 Not Found

Malpublished, deleted documents::

    >>> http_get("/publ/sfs/0")
    410 Gone

Some general error handling::

    >>> http_get("/publ/sfs/1999:175",
    ...         send_headers={"Accept": "application/msword"})
    406 Not Acceptable

    >>> http_get("/publ/sfs/1999:175/doc") # TODO: is this correct?
    415 Unsupported Media Type

    >>> http_get("/publ/sfs/1999:175?x=1")
    400 Bad Request

    >>> http_post("/publ/sfs/1999:175", data="")
    405 Method Not Allowed


Domain Specific Service Application Layer
========================================================================

.. TODO: för collection-feeds och algoritmiska sub-feeds:

    .. genererad "index.xhtml" för "/"-vy, t.ex.
            <http://rdata.lagrummet.se/publ/>

    .. atom-kategori-dokument för resp. collection
        <http://rdata.lagrummet.se/publ/categories>

        <http://rdata.lagrummet.se/feeds/publ/-/A>
        <http://rdata.lagrummet.se/textsearch/?q=Rättsinfo>
        <http://rdata.lagrummet.se/sparql/?query=>

.. TODO: utökad entry-åtkomst:

    <http://rdata.lagrummet.se/publ/sfs/1999:175/pdf,sv> =>
        "http://rdata.lagrummet.se/publ/sfs/1999:175/Rättsinformationsförordningen - SFS 1999:175.pdf"


