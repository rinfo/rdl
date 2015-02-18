package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchResponse
import se.lagrummet.rinfo.service.elasticsearch.RDLQueryBuilder

/**
 * Created by christian on 2/18/15.
 */
class ElasticQueryBuilderResult implements RDLQueryBuilder.Result {
    SearchResponse response
    String iriReplaceUrl

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
        return Utils.buildStats(response)
    }
}
