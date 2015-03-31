package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery
import org.elasticsearch.index.query.BoolFilterBuilder
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.OrFilterBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryStringQueryBuilder
import org.elasticsearch.index.query.functionscore.FunctionScoreModule
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryParser
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.search.MatchQuery
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.slf4j.LoggerFactory
import se.lagrummet.rinfo.service.elasticsearch.ElasticSearchQueryBuilder

/**
 * Created by christian on 2/18/15.
 */
class ElasticQueryBuilderQueryBuilder implements ElasticSearchQueryBuilder.QueryBuilder {
    private final logger = LoggerFactory.getLogger(ElasticQueryBuilderQueryBuilder)
    private ElasticSearchQueryBuilderImpl rdlQueryBuilder
    private int page
    private int pageSize
    private def types = []
    private def queries = []
    private def synonyms = []
    private Boolean explain

    ElasticQueryBuilderQueryBuilder(ElasticSearchQueryBuilderImpl rdlQueryBuilder) {
        this.rdlQueryBuilder = rdlQueryBuilder
    }

    @Override
    void addQuery(String queryText) {
        queries.add(queryText?.replaceAll(ElasticSearchQueryBuilder.regex_sanitize_elasticsearch, ElasticSearchQueryBuilder.replacement))
    }

    @Override
    void addSynonym(String synonym){
        synonyms.add(synonym?.replaceAll(ElasticSearchQueryBuilder.regex_sanitize_elasticsearch, ElasticSearchQueryBuilder.replacement))
    }

    @Override
    void restrictType(String type) {
        types.add(type)
    }

    @Override
    void setExplain(Boolean explain) {
        this.explain = explain
    }

    @Override
    void setPagination(int page, int pageSize) {
        this.pageSize = pageSize
        this.page = page
    }

    int startIndex() {page * pageSize}

    @Override
    void close() {}

    @Override
    ElasticSearchQueryBuilder.Result result(String iriReplaceUrl) {
        SearchRequestBuilder searchRequestBuilder = createAndPrepareSearchRequestBuilder()

        return new ElasticQueryBuilderResult(response: searchRequestBuilder.execute().actionGet(), iriReplaceUrl: iriReplaceUrl, page:page, pageSize:pageSize)
    }

    private SearchRequestBuilder createAndPrepareSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = rdlQueryBuilder.prepareSearch()

        searchRequestBuilder.explain = explain!=null?explain:false

        calculatePagination(searchRequestBuilder)
        setHighlightedFields(searchRequestBuilder, ElasticSearchQueryBuilder.HIGHLIGHTERS_TAG, ElasticSearchQueryBuilder.HIGHLIGHTED_FIELDS)
        searchRequestBuilder.addFields(ElasticSearchQueryBuilder.SELECT_FIELDS.tokenize(',').collect {it.trim()} as String[] )

        BoolQueryBuilderExplained boolQueryBuilderExplained = new BoolQueryBuilderExplained(
                ElasticSearchQueryBuilder.TYPE, ElasticSearchQueryBuilder.QUERY_SEARCH_FIELDS,
                ElasticSearchQueryBuilder.QUERY_MINIMUM_MATCH, ElasticSearchQueryBuilder.EXACT_MATCH_BOOST)
        queries.each boolQueryBuilderExplained.eachSearchQueryForPreSelectedSearchFields
        synonyms.each boolQueryBuilderExplained.eachSynonymQueryForPreSelectedSearchFields
        boolQueryBuilderExplained.imposeTo(searchRequestBuilder)

        if (!types.isEmpty())
            searchRequestBuilder.setPostFilter(addFilterForTypes("type",types))

        prepareGroupResultByType(searchRequestBuilder)
        addStatistics(searchRequestBuilder)
        logger.debug("elasticQuery:\n${searchRequestBuilder}")

        return searchRequestBuilder
    }

    private SearchRequestBuilder calculatePagination(SearchRequestBuilder searchRequestBuilder) {
        searchRequestBuilder.setFrom(startIndex())
        searchRequestBuilder.setSize(pageSize)
    }

    /* Helper functions */

    private static OrFilterBuilder createOrFilterByGroup(String group) {
        FilterBuilders.orFilter(ElasticSearchQueryBuilder.TYPE.findAll { it.group==group }.collect { FilterBuilders.termFilter("type", it.type) } as org.elasticsearch.index.query.FilterBuilder[])
    }

    private static SearchRequestBuilder prepareGroupResultByType(SearchRequestBuilder searchRequestBuilder) {
        searchRequestBuilder.addAggregation(
                AggregationBuilders
                        .filters("byType")
                        .filter("Lagar",createOrFilterByGroup("Lagar"))
                        .filter("Rattsfall",createOrFilterByGroup("Rattsfall"))
                        .subAggregation(
                        setHighlightedFields(
                                AggregationBuilders.topHits("top")
                                        .setFetchSource(ElasticSearchQueryBuilder.SELECT_FIELDS.tokenize(',').collect {it.trim()} as String[])
                                        .setSize(4)
                                ,ElasticSearchQueryBuilder.HIGHLIGHTERS_TAG
                                ,ElasticSearchQueryBuilder.HIGHLIGHTED_FIELDS
                        ) as AbstractAggregationBuilder
                )
        )

    }

    private static def addStatistics(SearchRequestBuilder searchRequestBuilder) {
        searchRequestBuilder.addAggregation(AggregationBuilders.terms("type").field("type"))
    }

    private static BoolFilterBuilder addFilterForTypes(String terms, def types) {
        return FilterBuilders.boolFilter().must(createOrFilterByTypes(terms, types))
    }

    private static OrFilterBuilder createOrFilterByTypes(String terms, def types) {
        FilterBuilders.orFilter(types.collect { FilterBuilders.termFilter(terms, it) } as org.elasticsearch.index.query.FilterBuilder[])
    }

    private static def setHighlightedFields(def searchRequestBuilder, highlighters_tag, highlighted_fields) {
        searchRequestBuilder.setHighlighterPreTags(highlighters_tag.start)
        searchRequestBuilder.setHighlighterPostTags(highlighters_tag.end)
        for (highlightedField in highlighted_fields) {
            searchRequestBuilder.addHighlightedField(highlightedField.field, highlightedField.size, highlightedField.number)
        }
        return searchRequestBuilder
    }

    /* Helper classes */

    private static class BoolQueryBuilderExplained {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        private List listOfTypesToBoost = []
        private String[] querySearchFields
        private String queryMinimalMatchPercent
        private Float exactMatchBoost

        BoolQueryBuilderExplained(List listOfTypesToBoost, String[] querySearchFields, String queryMinimalMatchPercent, Float exactMatchBoost) {
            this.exactMatchBoost = exactMatchBoost
            this.listOfTypesToBoost = listOfTypesToBoost
            this.queryMinimalMatchPercent = queryMinimalMatchPercent
            this.querySearchFields = querySearchFields
        }

        private def reduceScoreFilterForMultipleTitlesOnQuery() {
            QueryBuilders.functionScoreQuery(boolQuery,
                    ScoreFunctionBuilders.scriptFunction(
                            "if(_source.title instanceof List) { 1/pow(3, _source.title.size()) } else { 1 }"
                    )
            )
        }

        private void addBoostOfTypeInQuery(def functionScoreQuery) {
            listOfTypesToBoost.findAll { it.containsKey('boost') }.each {
                functionScoreQuery.add(
                        FilterBuilders.termFilter("type", it.type), ScoreFunctionBuilders.weightFactorFunction(it.boost as float)
                )
            }
        }

        def eachSearchQueryForPreSelectedSearchFields = {
            boolQuery.should(
                    QueryBuilders.multiMatchQuery(it, querySearchFields)
                            .type(MatchQuery.Type.PHRASE_PREFIX)
                            .minimumShouldMatch(queryMinimalMatchPercent)
                            .operator(MatchQueryBuilder.Operator.AND)
            )

            QueryStringQueryBuilder queryBuilder = QueryBuilders.queryString(it)
            querySearchFields.findAll {!it.startsWith("identifier") }.each {queryBuilder.field(it) }
            queryBuilder.defaultOperator(QueryStringQueryBuilder.Operator.OR)
            queryBuilder.analyzer("swedish_with_sfs_ids")
            queryBuilder.minimumShouldMatch(queryMinimalMatchPercent)
            queryBuilder.analyzeWildcard(true)
            boolQuery.should(queryBuilder)

            MatchQueryBuilder matchBuilder = QueryBuilders.matchQuery("identifier", it)
            matchBuilder.analyzer("identifiers")
            def idBoost = Float.parseFloat(querySearchFields.findAll {it.startsWith("identifier")}.first().tokenize("^")[1])
            matchBuilder.boost(idBoost)

            boolQuery.should(matchBuilder)

        }

        def eachSynonymQueryForPreSelectedSearchFields = {
            boolQuery.should(
                    QueryBuilders.multiMatchQuery(it, querySearchFields)
                            .operator(MatchQueryBuilder.Operator.AND)
                            .boost(2)
            )
        }

        void imposeTo(SearchRequestBuilder searchRequestBuilder) {
            def query = reduceScoreFilterForMultipleTitlesOnQuery()
            addBoostOfTypeInQuery(query)
            searchRequestBuilder.setQuery(query)
        }

    }
}
