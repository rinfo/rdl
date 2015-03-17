package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchResponse
import se.lagrummet.rinfo.service.elasticsearch.ElasticSearchQueryBuilder

/**
 * Created by christian on 2/18/15.
 */
class ElasticQueryBuilderResult implements ElasticSearchQueryBuilder.Result {
    SearchResponse response
    String iriReplaceUrl
    private int page
    private int pageSize

    @Override
    List items() {
        return Utils.createListOfSearchHits(response.getHits(), iriReplaceUrl)
    }

    @Override
    double duration() {
        return response.took.secondsFrac()
    }

    @Override
    Map stats() {
        return Utils.buildStats(response, iriReplaceUrl)
    }

    @Override int startIndex() {page*pageSize}
    @Override int pageSize() {pageSize}
    @Override int page() {page}

    @Override
    long totalHits() {
        return response.getHits().totalHits()
    }

    @Override
    int hitsLength() {
        return response.getHits().hits.length
    }


}
