package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.search.MatchQuery
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.search.facet.FacetBuilders
import se.lagrummet.rinfo.service.ElasticData
import se.lagrummet.rinfo.service.elasticsearch.RDLQueryBuilder


/**
 * Created by christian on 2/16/15.
 */
public class RDLQueryBuilderImpl implements RDLQueryBuilder {


    private ElasticData elasticData

    public RDLQueryBuilderImpl(ElasticData elasticData) {
        this.elasticData = elasticData
    }
    
    @Override
    RDLQueryBuilder.QueryBuilder createBuilder() {
        return new ElasticQueryBuilderQueryBuilder(this)
    }

    SearchRequestBuilder prepareSearch() {
        return elasticData.client.prepareSearch(PREPARED_SEARCH_NAME_INDICE)
    }

}

