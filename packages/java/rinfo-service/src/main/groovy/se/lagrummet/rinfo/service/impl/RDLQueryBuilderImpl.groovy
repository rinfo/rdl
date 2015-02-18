package se.lagrummet.rinfo.service.impl

import org.elasticsearch.index.query.BoolQueryBuilder
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
import se.lagrummet.rinfo.service.RDLQueryBuilder


/**
 * Created by christian on 2/16/15.
 */
class RDLQueryBuilderImpl implements RDLQueryBuilder {

    private final float TYPE_BOOST = 1.05f
    private final String QUERY_MINIMUM_MATCH = "80%"


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
        RDLQueryBuilder.Result result() {
            SearchRequestBuilder searchRequestBuilder = client.prepareSearch("rinfo")
            searchRequestBuilder.setSize(hitcount)
                    .setHighlighterPreTags("<em class=\"match\">")
                    .setHighlighterPostTags("</em>")
                    .addHighlightedField("title", 150, 0)
                    .addHighlightedField("identifier", 150, 0)
                    .addHighlightedField("text", 150, 0)
                    .addHighlightedField("referatrubrik", 150, 0)
            searchRequestBuilder.setQuery(boolQuery)
            return new ElasticQueryBuilderResult(response: searchRequestBuilder.execute().actionGet())
        }

        private class ElasticQueryBuilderResult implements RDLQueryBuilder.Result {
            SearchResponse response

            @Override
            List items() {
                return createListOfSearchHits(response.getHits())
            }
        }

        @Override
        void close() {

        }
    }

    private static List createListOfSearchHits(SearchHits hits) {
        def list = []
        for (def hit : hits) {
            list.add(hit.getSource())
        }
        return list
    }

    public static void main(String[] properties){
        RDLQueryBuilderImpl elasticQueryBuilder = new RDLQueryBuilderImpl()
        def qb = elasticQueryBuilder.createBuilder()
        qb.addQuery("Rättsinformationsförordning")
        def res = qb.result()
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(res.items());
            println "*************************************************************************************************"
            println json
            println "*************************************************************************************************"
            //println "Träffar "+countHits+"\n"+json
        }
        catch(JsonProcessingException ex) {
            ex.printStackTrace()
        }
    }
}
