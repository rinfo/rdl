package se.lagrummet.rinfo.service

import groovy.transform.CompileStatic
import org.restlet.data.Reference

class SimpleElasticQuery {

    ElasticData elasticData
    String serviceAppBaseUrl
    RDLQueryBuilder builder;

    SimpleElasticQuery(ElasticData elasticData, String serviceAppBaseUrl) {
        this.elasticData = elasticData
        this.serviceAppBaseUrl = serviceAppBaseUrl
        this.builder = builder;
    }

    Map search(docType, reference) {
        println '++++++++++++++++++++++++++++++++++++++++++++++++++++++ simpleElasticQuery search ++++++++++++++++++++++++++++++++++++++++++++++++++++++++'

        def qb = builder.createBuilder()
        try {

            reference.get
            qb.addQuery("Rättsinformationsförordningen")
            qb.restrictType(RDLQueryBuilder.Type.KonsolideradGrundforfattning)

            return [
                    "@language" : "sv",
                    "@context" : "/json-ld/context.json",
                    "startIndex" : 0,
                    "itemsPerPage" : 50,
                    "totalResults" : 2,
                    "duration" : "PT0.023S",
                    "current" : "/-/publ?_page=0&q=r%C3%A4ttsinformationsf%C3%B6rordning",
                    items:qb.result().items()
            ]
        } finally {
            qb.close()
        }

    }

    Map staticResult() {
        Map res = [
                "@language" : "sv",
                "@context" : "/json-ld/context.json",
                "startIndex" : 0,
                "itemsPerPage" : 50,
                "totalResults" : 2,
                "duration" : "PT0.023S",
                "current" : "/-/publ?_page=0&q=r%C3%A4ttsinformationsf%C3%B6rordning",
                "items" : [ [
                        "title" : "Rättsinformationsförordning (1999:175)",
                        "type" : "KonsolideradGrundforfattning",
                        "identifier" : "SFS 1999:175 i lydelse enligt SFS 2010:1990",
                        "konsoliderar" : [
                                "iri" : "http://rinfo.lagrummet.se/publ/sfs/1999:175"
                        ],
                        "iri" : "http://rinfo.lagrummet.se/publ/sfs/1999:175/konsolidering/2011-05-02",
                        "issued" : "2011-05-02",
                        "describedby" : "http://localhost:8181/publ/sfs/1999:175/konsolidering/2011-05-02/data.json",
                        "matches" : [
                                "text" : [ "<em class=\"match\">Rättsinformationsförordning</em> (1999:175)Ett offentligt rättsinformationssystem  1 § I syfte att tillförsäkra den offentliga förvaltningen och enskilda" ],
                                "title" : [ "<em class=\"match\">Rättsinformationsförordning</em> (1999:175)" ]
                        ]
                ], [
                        "publisher" : [
                                "iri" : "http://rinfo.lagrummet.se/org/regeringskansliet"
                        ],
                        "rev" : [
                                "andrar.iri" : [ "http://rinfo.lagrummet.se/publ/sfs/2008:763", "http://rinfo.lagrummet.se/publ/sfs/2010:1990", "http://rinfo.lagrummet.se/publ/sfs/2008:1205" ],
                                "konsoliderar.iri" : "http://rinfo.lagrummet.se/publ/sfs/1999:175/konsolidering/2011-05-02"
                        ],
                        "forfattningssamling" : [
                                "iri" : "http://rinfo.lagrummet.se/serie/fs/sfs"
                        ],
                        "issued" : "1999-03-25",
                        "type" : "Forordning",
                        "iri" : "http://rinfo.lagrummet.se/publ/sfs/1999:175",
                        "title" : "Rättsinformationsförordning (1999:175)",
                        "ikrafttradandedatum" : "1999-05-01",
                        "utfardandedatum" : "1999-03-25",
                        "upphaver" : [
                                "iri" : "http://rinfo.lagrummet.se/publ/sfs/1980:628"
                        ],
                        "identifier" : "SFS 1999:175",
                        "andrar" : [
                                "iri" : "http://rinfo.lagrummet.se/publ/sfs/1980:628"
                        ],
                        "describedby" : "http://localhost:8181/publ/sfs/1999:175/data.json",
                        "matches" : [
                                "text" : [ "0175Svensk författningssamling SFS 1999:175 <em class=\"match\">Rättsinformationsförordning</em>; Utkom från trycket den 13 april 1999 utfärdad den 25 mars 1999. Regeringen" ],
                                "title" : [ "<em class=\"match\">Rättsinformationsförordning</em> (1999:175)" ]
                        ]
                ] ]
        ]
        return res
    }
}
