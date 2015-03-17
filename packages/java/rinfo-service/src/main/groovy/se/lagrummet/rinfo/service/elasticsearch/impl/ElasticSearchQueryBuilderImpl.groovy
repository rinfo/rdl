package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchRequestBuilder
import se.lagrummet.rinfo.service.ElasticData
import se.lagrummet.rinfo.service.elasticsearch.ElasticSearchQueryBuilder


/**
 * Created by christian on 2/16/15.
 */
public class ElasticSearchQueryBuilderImpl implements ElasticSearchQueryBuilder {


    private ElasticData elasticData

    public ElasticSearchQueryBuilderImpl(ElasticData elasticData) {
        this.elasticData = elasticData
    }
    
    @Override
    ElasticSearchQueryBuilder.QueryBuilder createBuilder() {
        return new ElasticQueryBuilderQueryBuilder(this)
    }

    SearchRequestBuilder prepareSearch() {
        return elasticData.client.prepareSearch(elasticData.indexName)
    }

}

