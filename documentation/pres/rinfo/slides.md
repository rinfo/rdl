# Det svenska rättsinformationssystemet #

------------------------------------------

# Domänen

* Regering och riksdag: utfärdar förordningar och lagar
* 100-talet myndigheter: publicerar föreskrifter
* Domstolar: rättsfall som leder till avgöranden
* Massor av dokument

<style>
body { font-size: 23px; }
.err { border: none; }
</style>

------------------------------------------

# Situationen

* Publikationsansvar: respektive myndighet
* Vidareutnyttjare: samlar data så gott det går

------------------------------------------

# Behoven

* Förordning: SFS 1999:175
* Koherens: beskrivningar
* Hållbarhet: URIer

------------------------------------------

# Data

------------------------------------------

<img src="rinfo-relations.png"
     style="position: absolute; top: 3em;">

------------------------------------------

    !turtle

    @prefix : <http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#> .
    @prefix dc: <http://purl.org/dc/terms/> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

    <http://rinfo.lagrummet.se/publ/ra-fs/2004:2> a :Myndighetsforeskrift;

        :forfattningssamling <http://rinfo.lagrummet.se/serie/fs/ra-fs>;
        :arsutgava "2004";
        :lopnummer "2";

        dc:identifier "RA-FS 2004:2";
        dc:title """Riksarkivets föreskrifter och allmänna råd om
            gallring och återlämnande av handlingar vid upphandling;"""@sv;
        dc:publisher <http://rinfo.lagrummet.se/org/riksarkivet>;

        :beslutadAv <http://rinfo.lagrummet.se/org/riksarkivet>;
        :bemyndigande <http://rinfo.lagrummet.se/publ/sfs/1991:446#p_12>;

        :beslutsdatum "2004-08-30"^^xsd:date;
        :utkomFranTryck "2004-09-27"^^xsd:date;
        :ikrafttradandedatum "2004-11-01"^^xsd:date .

------------------------------------------

# Modellen

<http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#>

------------------------------------------

# Inhämtning

------------------------------------------

# Hantering

* **Listor**, av...
* **Poster**, som paketerar...
* **Dokument**, som kan beskrivas med...
* **Egenskaper** och länkar

------------------------------------------

# Syndikering

* Lista: **Atom feed**
* Post: **Atom entry**
* Dokument: **PDF**, *HTML+RDFa*
* Beskrivning: **RDF** (RDF/XML)


------------------------------------------

<img src="rinfo-oversikt.png"
     style="position: absolute; top: 0em;">

------------------------------------------

# Atom

------------------------------------------

# Poster

    !xml
    <entry>

      <id>http://rinfo.lagrummet.se/publ/ra-fs/2004:2</id>
      <updated>2004-09-27T00:00:00.000Z</updated>
      <published>2004-09-27T00:00:00.000Z</published>

      <title>Riksarkivets föreskrifter och allmänna råd om gallring och
          återlämnande av handlingar vid upphandling;</title>
      <summary></summary>

      <content src="https://www.statensarkiv.se/Sve/RAFS/Filer/ra-fs-2004-02.pdf"
              type="application/pdf"
              hash="md5:ca68b77f41ad2231586cf3e4d7970629"/>

      <link rel="alternate"
            href="https://www.statensarkiv.se/Sve/RAFS/showrdf?doc=2004-2"
            type="application/rdf+xml"
            length="2493" hash="md5:af7e5154dc653296506564d0b038697c"/>

    </entry>


------------------------------------------

# Feeds

* RFC 4287: The Atom Syndication Format
* RFC 5005: Feed Paging and Archiving
* Tombstones I-D: The Atom "deleted-entry" Element

------------------------------------------

# Kompletta

    !xml
    <feed xmlns="http://www.w3.org/2005/Atom"
          xmlns:fh="http://purl.org/syndication/history/1.0">

      <fh:complete/>

------------------------------------------

# Arkiv + raderingar

    !xml

    <feed xmlns="http://www.w3.org/2005/Atom"
          xmlns:at="http://purl.org/atompub/tombstones/1.0">

      <link rel="prev-archive"
            href="https://www.statensarkiv.se/rafs/feed/archive/2008/index.atom"/>

      <at:deleted-entry
          ref="http://rinfo.lagrummet.se/publ/ra-fs/3010:0"
          when="2010-03-08T15:55:34+0100"/>

------------------------------------------

# Datadrivet

------------------------------------------

# VoID

    !turtle

    <http://rinfo.lagrummet.se/sys/dataset> a void:Dataset;

        dc:publisher <http://rinfo.lagrummet.se/org/domstolsverket>;
        foaf:homepage <http://rinfo.lagrummet.se/>;
        void:vocabulary rpubl:, dc:, foaf:;

        iana:describedby <http://rinfo.lagrummet.se/sys/uri/space#>;
        iana:describedby <http://rinfo.lagrummet.se/sys/validation/>;

        void:dataDump [
                iana:current <http://rinfo.lagrummet.se/feed/current>;
                dc:identifier "tag:lagrummet.se,2009:rinfo"^^xsd:anyURI;
                dc:format "application/atom+xml"
            ];

        dc:source [
                dc:publisher <http://rinfo.lagrummet.se/org/boverket>;
                iana:current <https://rinfo.boverket.se/index.atom>;
                dc:identifier "tag:boverket.se,2009:rinfo:feed"^^xsd:anyURI;
                dc:format "application/atom+xml"
            ],

        # ...

------------------------------------------

# Validering

------------------------------------------

# Schemarama

* Bakgrundsdata (modeller)
* Ladda resursbeskrivningen
* Använd SPARQL 1.1 för att kontrollera
* Skapa felmeddelanden (`CONSTRUCT`)

------------------------------------------

# Finns text på svenska?

    !sparql

    PREFIX sch: <http://purl.org/net/schemarama#>

    CONSTRUCT {

        [] a sch:Warning;
            rdfs:isDefinedBy <http://rinfo.lagrummet.se/sys/validation/expected_lang.rq>;
            sch:message """Resurs [1]: förväntade något värde för egenskap [2] 
                           på svenska, fann [3]."""@sv;
            sch:implicated (?thing ?prop ?value);
            dct:source ?context .

    } WHERE {
        GRAPH ?context {
            ?thing ?prop ?value .
            FILTER(?prop in (dct:title, dct:description))
            MINUS {
                ?thing ?prop ?anyvalue .
                FILTER(langMatches(lang(?anyvalue), 'sv'))
            }
        }
    }

------------------------------------------

# Verifiera kardinalitet

    !sparql

    CONSTRUCT {

        [] a sch:Warning;
            sch:message """Resurs [1]: inget värde angivet för egenskap [2] 
                           (ska vara minst [3])."""@sv;
            sch:implicated (?thing ?baseprop ?cardinality) .

    } WHERE {

        ?restr a owl:Restriction;
            owl:onProperty ?baseprop;
            (owl:minCardinality | owl:cardinality) ?cardinality .

        FILTER(?cardinality > 0)

        GRAPH ?context { ?thing a ?type . }

        ?type rdfs:subClassOf+ ?restr .

        FILTER NOT EXISTS {
            ?prop rdfs:subPropertyOf* ?baseprop .
            GRAPH ?context { ?thing ?prop ?value . }
        }

    }

------------------------------------------

# URI-rymden

------------------------------------------

# Varianter

    http://rinfo.lagrummet.se/publ/sfs/1999:175
    http://rinfo.lagrummet.se/publ/sfs/1899:bih_40_s_3
    http://rinfo.lagrummet.se/publ/saeifs/1983:1
    http://rinfo.lagrummet.se/publ/sfs/1999:175/konsolidering/1999-03-25
    http://rinfo.lagrummet.se/publ/tsfs/2010:182
    http://rinfo.lagrummet.se/publ/rf/nja/2005:57
    http://rinfo.lagrummet.se/publ/rf/nja/2005/s_523
    http://rinfo.lagrummet.se/publ/rf/nja/1913/not/b_418
    http://rinfo.lagrummet.se/publ/dom/hd/b333-04/2005-06-17
    http://rinfo.lagrummet.se/publ/avg/jk/874-05-30
    http://rinfo.lagrummet.se/publ/dir/1999:42
    http://rinfo.lagrummet.se/publ/utr/ds/2005:26
    http://rinfo.lagrummet.se/publ/utr/sou/2000:114
    http://rinfo.lagrummet.se/publ/fm/2005:2
    http://rinfo.lagrummet.se/publ/prop/2002/03:20
    http://rinfo.lagrummet.se/publ/prop/1975:95

    ... många fler ...

------------------------------------------

# CoIN

Composition of Identifier Names

------------------------------------------

    !turtle

    @prefix coin: <http://purl.org/court/def/2009/coin#> .
    @prefix : <http://rinfo.lagrummet.se/sys/uri/space#> .

    : a coin:URISpace;
        coin:base "http://rinfo.lagrummet.se";
        coin:slugTransform [
                coin:apply coin:ToLowerCase;
                coin:replace "é e", "å aa", "ä ae", "ö oe";
                coin:spaceReplacement "_";
            ];

        coin:template [
            coin:uriTemplate "/publ/{fs}/{arsutgava}:{lopnummer}";
            coin:binding [
                    coin:variable "fs";
                    coin:property rpubl:forfattningssamling;
                    coin:slugFrom skos:altLabel
                ], [
                    coin:variable "arsutgava";
                    coin:property rpubl:arsutgava
                ], [
                    coin:variable "lopnummer";
                    coin:property rpubl:lopnummer
                ]
        ],

------------------------------------------

# Dokumentation

<http://dev.lagrummet.se/dokumentation/>

