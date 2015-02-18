package se.lagrummet.rinfo.service.impl

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.map.ObjectWriter
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.codehaus.jackson.map.ObjectMapper
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.search.MatchQuery
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet
import se.lagrummet.rinfo.service.RDLQueryBuilder


/**
 * Created by christian on 2/16/15.
 */
class RDLQueryBuilderImpl implements RDLQueryBuilder {

    private final float TYPE_BOOST = 1.05f
    private final String QUERY_MINIMUM_MATCH = "80%"

    private final String SELECT_FIELDS = "type,iri,identifier,title,malnummer,diarienummer,utfardandedatum," +
            "beslutsdatum,issued,utkomFranTryck,ikrafttradandedatum,avgorandedatum,ratificeringsdatum," +
            "publisher.iri,forfattningssamling.iri,utrSerie.iri,rattsfallspublikation.iri," +
            "allmannaRadSerie.iri,andrar.iri,ersatter.iri,upphaver.iri,konsoliderar.iri,rev.konsoliderar.iri," +
            "rev.ersatter.iri,rev.andrar.iri,rev.upphaver.iri";

    private final String SELECT_FIELDS_BRIEF = "type,iri,identifier,title,malnummer,beslutsdatum,issued," +
            "ikrafttradandedatum"


    TransportClient client


    RDLQueryBuilderImpl() {
        this("localhost",9300)
    }

    RDLQueryBuilderImpl(String host, int port) {
        client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host, port))
    }

    @Override
    RDLQueryBuilder.QueryBuilder createBuilder() {
        return new ElasticQueryBuilderQueryBuilder(6)
    }

    private class ElasticQueryBuilderQueryBuilder implements RDLQueryBuilder.QueryBuilder {

        BoolQueryBuilder boolQuery
        private int hitcount

        ElasticQueryBuilderQueryBuilder(int hitcount) {
            this.hitcount = hitcount
            boolQuery = QueryBuilders.boolQuery()
        }

        @Override
        void addQuery(String querytext) {
            boolQuery.should(
                    QueryBuilders.multiMatchQuery(
                            querytext,
                            "identifier^5",
                            "title^2",
                            "text"
                    ).type(MatchQuery.Type.PHRASE_PREFIX)
                     .minimumShouldMatch(QUERY_MINIMUM_MATCH)
            )
        }

        @Override
        void restrictType(RDLQueryBuilder.Type type) {
            boolQuery.should(
                    QueryBuilders.constantScoreQuery(
                            FilterBuilders.termFilter("type",type)
                    ).boost(TYPE_BOOST)
            )
        }

        @Override
        RDLQueryBuilder.Result result(String iriReplaceUrl) {
            SearchRequestBuilder searchRequestBuilder = client.prepareSearch("rinfo")
            searchRequestBuilder.setSize(hitcount)
                    .setHighlighterPreTags("<em class=\"match\">")
                    .setHighlighterPostTags("</em>")
                    .addHighlightedField("title", 150, 0)
                    .addHighlightedField("identifier", 150, 0)
                    .addHighlightedField("text", 150, 0)
                    .addHighlightedField("referatrubrik", 150, 0)
            //searchRequestBuilder.addFields(SELECT_FIELDS.tokenize(',') as String[])
            searchRequestBuilder.addFields(SELECT_FIELDS_BRIEF.tokenize(',') as String[])
            //searchRequestBuilder.addFields("title")
            searchRequestBuilder.setQuery(boolQuery)
            searchRequestBuilder.addFacet(FacetBuilders.termsFacet("type").field("type"))
            println searchRequestBuilder
            return new ElasticQueryBuilderResult(response: searchRequestBuilder.execute().actionGet(), iriReplaceUrl: iriReplaceUrl)
        }

        private class ElasticQueryBuilderResult implements RDLQueryBuilder.Result {
            SearchResponse response
            String iriReplaceUrl

            @Override
            List items() {
                return createListOfSearchHits(response.getHits(), iriReplaceUrl)
            }

            @Override
            double duration() {
                return response.took.secondsFrac()
            }

            @Override
            Map stats() {
                return buildStats(response)
            }
        }

        @Override
        void close() {

        }
    }

    /* Static methods */

    static Map buildStats(SearchResponse esRes) {
        return [
                type: "DataSet",
                slices: esRes.facets.collect {
                    def iriPos = it.name.indexOf(".iri")
                    def isIri = iriPos > -1
                    return [
                            dimension: isIri? it.name.substring(0, iriPos) : it.name,
                            observations: it.entries.collect {
                                def isDate = it instanceof DateHistogramFacet.Entry
                                def key = isDate? "year" : isIri? "ref" : "term"
                                def value = isDate? 1900 + new Date(it.time).year : it.term.toString()
                                return [(key): value, count: it.count]
                            }
                    ]
                }.findAll {
                    it.observations
                }
        ]
    }

    static List createListOfSearchHits(SearchHits hits, String iriReplaceUrl) {
        def list = []
        for (def hit : hits) {
            println "hit="+hit.toString()
            list.add( buildResultItem(hit, iriReplaceUrl))
        }
        return list
    }

    static Map buildResultItem(SearchHit hit, String iriReplaceUrl) {
        def item = [:]
        hit.fields.each { key, hf ->
            def lItem = item
            def lKey = key
            def dotAt = key.indexOf('.')
            if (dotAt > -1) {
                def baseKey = key.substring(0, dotAt)
                lItem = item.get(baseKey, [:])
                lKey = key.substring(dotAt + 1)
            }
            if (hf.value != null) {
                lItem[lKey] = hf.values.size() > 1?  hf.values : hf.value
            }
        }
        if (item.iri) {
            item.describedby = makeServiceLink(item.iri, iriReplaceUrl)
        }
        hit.highlightFields.each { key, hlf ->
            item.get('matches', [:])[key] = hlf.fragments.collect { it.toString() }
        }
        return item
    }

    static String makeServiceLink(String iri, String iriReplaceUrl) {
        // TODO: Experimental. Use base from request? Link to alt mediaType versions?
        return iri.replaceFirst(/http:\/\/rinfo\.lagrummet\.se\/([^#]+)(#.*)?/, iriReplaceUrl + '$1/data.json$2')
    }

    /* Test run */

    public static void main(String[] properties){
        RDLQueryBuilderImpl elasticQueryBuilder = new RDLQueryBuilderImpl()
        def qb = elasticQueryBuilder.createBuilder()
        qb.addQuery("Rättsinformationsförordning")
        def res = qb.result()
        println "*************************************************************************************************"
        println res.items().toString()
        println "*************************************************************************************************"
        println "duration=" + res.duration()
        println "*************************************************************************************************"
        println res.stats().toMapString()
        println "*************************************************************************************************"
    }
}

/*{
    "type": "Rattsinformationsdokument",
    "iri": null,
    "identifier": {"_boost": 100},
    "title": {"_boost": 20},

    "malnummer": null,
    "diarienummer": null,

    "utfardandedatum": null,
    "beslutsdatum": null,
    "issued": null,
    "utkomFranTryck": null,
    "ikrafttradandedatum": null,
    "avgorandedatum": null,
    "ratificeringsdatum": null,

    "publisher": {"iri": null},

    "forfattningssamling": {"iri": null},
    "utrSerie": {"iri": null},
    "rattsfallspublikation": {"iri": null},
    "allmannaRadSerie": {"iri": null},

    "andrar": {"iri": null},
    "ersatter": {"iri": null},
    "upphaver": {"iri": null},
    "konsoliderar": {"iri": null},

    "rev": {
        "konsoliderar": {"iri": null},
        "ersatter": {"iri": null},
        "andrar": {"iri": null},
        "upphaver": {"iri": null}
    }
  }

[publ:[type, iri, identifier, title, malnummer, diarienummer, utfardandedatum, beslutsdatum, issued, utkomFranTryck, ikrafttradandedatum, avgorandedatum, ratificeringsdatum, publisher.iri, forfattningssamling.iri, utrSerie.iri, rattsfallspublikation.iri, allmannaRadSerie.iri, andrar.iri, ersatter.iri, upphaver.iri, konsoliderar.iri, rev.konsoliderar.iri, rev.ersatter.iri, rev.andrar.iri, rev.upphaver.iri],
  org:[type, iri, name],
serie:[type, iri, altLabel, prefLabel, publisher.iri], ns:[], sys:[], ext:[]]  */

