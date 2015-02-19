package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.search.MatchQuery
import org.elasticsearch.search.facet.FacetBuilders
import se.lagrummet.rinfo.service.elasticsearch.RDLQueryBuilder

/**
 * Created by christian on 2/18/15.
 */
class ElasticQueryBuilderQueryBuilder implements RDLQueryBuilder.QueryBuilder {

    BoolQueryBuilder boolQuery
    private RDLQueryBuilderImpl rdlQueryBuilder
    private int page
    private int pageSize
    private def types = []

    ElasticQueryBuilderQueryBuilder(RDLQueryBuilderImpl rdlQueryBuilder) {
        this.rdlQueryBuilder = rdlQueryBuilder
        boolQuery = QueryBuilders.boolQuery()
    }

    @Override
    void addQuery(String querytext) {
        addSearchQueryForPreSelectedSearchFields(querytext, RDLQueryBuilder.QUERY_SEARCH_FIELDS, RDLQueryBuilder.QUERY_MINIMUM_MATCH)
    }

    @Override
    void restrictType(String type) {
        types.add(type)
    }

    @Override
    RDLQueryBuilder.Result result(String iriReplaceUrl) {
        boostTypeInSearch(RDLQueryBuilder.Type.KonsolideradGrundforfattning, RDLQueryBuilder.TYPE_BOOST_KONSOLIDERAD_GRUNDFORFATTNING)

        SearchRequestBuilder searchRequestBuilder = createAndPrepareSearchRequestBuilder()

        return new ElasticQueryBuilderResult(response: searchRequestBuilder.execute().actionGet(), iriReplaceUrl: iriReplaceUrl, page:page, pageSize:pageSize)
    }

    private SearchRequestBuilder createAndPrepareSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = rdlQueryBuilder.prepareSearch()

        calculatePagination(searchRequestBuilder)
        setHighlightedFields(searchRequestBuilder, RDLQueryBuilder.HIGHLIGHTERS_TAG, RDLQueryBuilder.HIGHLIGHTED_FIELDS)
        searchRequestBuilder.addFields(RDLQueryBuilder.SELECT_FIELDS.tokenize(',').collect {it.trim()} as String[] )
        if (!types.isEmpty())
            addSearchQueryForPreSelectedSearchFields(types.toListString(),["type"] as String[],"100%");
        searchRequestBuilder.setQuery(boolQuery)
        searchRequestBuilder.addFacet(FacetBuilders.termsFacet("type").field("type"))

        return searchRequestBuilder
    }

    @Override
    void setPagination(int page, int pageSize) {
        this.pageSize = pageSize
        this.page = page
    }

    int startIndex() {page * pageSize}

    @Override
    void close() {}

    private BoolQueryBuilder addSearchQueryForPreSelectedSearchFields(String queryText, String[] searchFields, String minimalMatchPercent) {
        boolQuery.should(
                QueryBuilders.multiMatchQuery(queryText, searchFields)
                        .type(MatchQuery.Type.PHRASE_PREFIX)
                        .minimumShouldMatch(minimalMatchPercent)
        )
    }

    private BoolQueryBuilder boostTypeInSearch(RDLQueryBuilder.Type type, float boostValue) {
        boolQuery.should(
                QueryBuilders.constantScoreQuery(FilterBuilders.termFilter("type", type))
                        .boost(boostValue)
        )
    }

    private SearchRequestBuilder calculatePagination(SearchRequestBuilder searchRequestBuilder) {
        println "se.lagrummet.rinfo.service.elasticsearch.impl.ElasticQueryBuilderQueryBuilder.calculatePagination startIndex=${startIndex()} pageSize=${pageSize}"
        searchRequestBuilder.setFrom(startIndex())
        searchRequestBuilder.setSize(pageSize)
    }

    private static void setHighlightedFields(SearchRequestBuilder searchRequestBuilder, highlighters_tag, highlighted_fields) {
        println "se.lagrummet.rinfo.service.elasticsearch.impl.ElasticQueryBuilderQueryBuilder.setHighlightedFields"
        searchRequestBuilder.setHighlighterPreTags(highlighters_tag.start)
        searchRequestBuilder.setHighlighterPostTags(highlighters_tag.end)
        for (highlightedField in highlighted_fields) {
            println "se.lagrummet.rinfo.service.elasticsearch.impl.ElasticQueryBuilderQueryBuilder.setHighlightedFields field=${highlightedField.field} size=${highlightedField.size} number=${highlightedField.number}"
            searchRequestBuilder.addHighlightedField(highlightedField.field, highlightedField.size, highlightedField.number)
        }
    }
}
