# RInfo Service

------------------------------------------

# Index

* Läser rättsdatalagrets Atom-flöde
* Indexerar respektive post

<style>
body { font-size: 23px; }
.err { border: none; }
</style>

------------------------------------------

# Serve

* Exponerar varje resurs med berikad (kontextuell) data
* På formaten:
    - JSON-LD
    - Turtle
    - RDF/XML

------------------------------------------

# Rika poster

* Quad-store (Sesame 2.6)
* SPARQL 1.1
* <strike><code>DESCRIBE? CBD?</code></strike>
* <code>CONSTRUCT</code> med domänspecifika behov

------------------------------------------

# Domänspecifika vyer

    !sparql

    #...

    } UNION {

        ?current a ?type .
        FILTER NOT EXISTS {
            ?type rdfs:subClassOf* ?basetype .
            FILTER(?basetype in (foaf:Organization, bibo:Collection))
        }

        ?revitem ?rev ?current .

        {
            ?revitem ?revprop ?revtext .
            FILTER(isLiteral(?revtext))
            OPTIONAL { ?revitem a ?revtype . }
            OPTIONAL { ?revitem dct:publisher ?revpublisher . }
        } UNION {
            ?revrevitem ?revrev ?revitem;
                ?revrevprop ?revrevtext .
            FILTER (?revrev = rpubl:referatAvDomstolsavgorande &&
                    isLiteral(?revrevtext) )
            OPTIONAL { ?revrevitem a ?revrevtype . }
        }

    #...

------------------------------------------

# .. disconnect...

    http://rinfo.lagrummet.se/publ/sfs/1999:175

    http://service.lagrummet.se/publ/sfs/1999:175/data

------------------------------------------

# Query API

* För varje entry skapas en indexpost (JSON-LD)
* Denna indexeras i ElasticSearch (bygger på Lucene)
* Bläddring/filtrering/sökning (JSON-LD)

------------------------------------------

    !js
    {
      "@context" : "/json-ld/context.json",
      "startIndex" : 0,
      "itemsPerPage" : 10,
      "totalResults" : 34,
      "duration" : "PT0.074S",
      "current" : "/-/publ?q=RA-FS&_stats=on&_page=0&_pageSize=10",
      "next" : "/-/publ?q=RA-FS&_stats=on&_page=1&_pageSize=10",
      "items" : [ {
        "identifier" : "RA-FS 1991:1",
        "title" : "Riksarkivets föreskrifter och allmänna råd om arkiv hos statliga myndigheter",
        "beslutsdatum" : "1991-05-23",
        "utkomFranTryck" : "1991-06-25",
        "ikrafttradandedatum" : "1991-07-01",
        "type" : "Myndighetsforeskrift",
        "forfattningssamling" : {"iri" : "http://rinfo.lagrummet.se/serie/fs/ra-fs"},
        "iri" : "http://rinfo.lagrummet.se/publ/ra-fs/1991:1",
        "publisher" : {"iri" : "http://rinfo.lagrummet.se/org/riksarkivet"},
        "describedby" : "http://service.demo.lagrummet.se/publ/ra-fs/1991:1/data.json"
      }, {
        ...

------------------------------------------

# <code>http://service.lagrummet.se/-/</code>

    /-/publ?q=eko*

    /-/publ?title=*djur*

    /-/publ?type=Lag&type=Forordning

    /-/publ?publisher.iri=*/finansinspektionen


------------------------------------------

Grundförfattningar som var ikraft första januari 2011:

    /-/publ.json?max-ikrafttradandedatum=2011-01-01&
        ifExists-minEx-rev.upphaver.ikrafttradandedatum=2011-01-01&
        exists-andrar.iri=false&
        exists-upphaver.iri=false


------------------------------------------

Sortering och paginering:

    /-/publ?forfattningssamling.iri=*/fffs&_sort=-beslutsdatum

    /-/publ?forfattningssamling.iri=*/fffs&_pageSize=20&_page=1

------------------------------------------

Facetter:

    /-/publ;stats

    /-/publ?type=Myndighetsforeskrift&_stats=on


------------------------------------------

    !js

    "statistics" : {
        "type" : "DataSet",
        "slices" : [ {
        "dimension" : "type",
        "observations" : [ {
            "term" : "Proposition",
            "count" : 27
        }, {
            "term" : "Myndighetsforeskrift",
            "count" : 4
        }, {
            "term" : "Forordning",
            "count" : 2
        }, {
            "term" : "Rattsfallsreferat",
            "count" : 1
        } ]
        }, {
        "dimension" : "utkomFranTryck",
        "observations" : [ {
            "year" : 1991,
            "count" : 1
        }, {
            "year" : 1998,
            "count" : 1
            ...

------------------------------------------

# API-utforskare

<http://service.demo.lagrummet.se/ui/>

