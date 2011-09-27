package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j as Log

import org.restlet.Context
import static org.restlet.data.CharacterSet.UTF_8
import org.restlet.data.MediaType
import org.restlet.data.Reference
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.StringRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.Finder
import org.restlet.resource.Get
import org.restlet.resource.ServerResource

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.client.action.search.*
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryStringQueryBuilder
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet
import org.elasticsearch.search.sort.SortOrder

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig


@Log
class ElasticFinder extends Finder {

    ElasticData elasticData
    String serviceAppBaseUrl

    def contextMap
    def jsonMapper

    def defaultPageSize = 50
    def pageParamKey = '_page'
    def pageSizeParamKey = '_pageSize'
    def facetStatsSegment = "stats"

    ElasticFinder(Context context, ElasticData elasticData, String serviceAppBaseUrl) {
        super(context)
        this.elasticData = elasticData
        this.serviceAppBaseUrl = serviceAppBaseUrl
        jsonMapper = new ObjectMapper()
        jsonMapper.configure(
                SerializationConfig.Feature.INDENT_OUTPUT, true)
    }

    @Override
    ServerResource find(Request request, Response response) {
        final String collection = request.attributes["collection"]
        SearchRequestBuilder srb = elasticData.client.prepareSearch(elasticData.indexName)
        def data = (collection == facetStatsSegment)?
            getElasticStats(srb) :
            searchElastic(srb, collection, request.resourceRef)
        return new ServerResource() {
            @Get("json")
            Representation asJSON() {
                def jsonStr = jsonMapper.writeValueAsString(data)
                def mediaType = MediaType.APPLICATION_JSON
                return new StringRepresentation(jsonStr, mediaType, null, UTF_8)
            }
        }
    }

    def searchElastic(srb, collection, ref) {
        def search = prepareElasticSearch(srb, ref, elasticData.listTerms) // TODO: showFieldsby collection?

        SearchResponse esRes = srb.execute().actionGet()
        assert esRes.failedShards == 0

        def data = [
            "@language": "sv",
            "@context": "/json-ld/list-context.json",
            startIndex: search.startIndex,
            itemsPerPage: search.pageSize,
            totalResults: esRes.hits.totalHits(),
            duration: "PT${esRes.took.secondsFrac}S" as String,
        ]

        def currentPage = "/-/${collection}?${search.queryString}"
        def pageParam = pageParamKey + '=' + search.page
        if (search.page > 0) {
            data.prev = currentPage.replace(pageParam, pageParamKey + '=' + (search.page - 1))
        }
        data.current = currentPage as String
        if (esRes.hits.hits.length == data.itemsPerPage &&
                (data.startIndex + data.itemsPerPage) < data.totalResults) {
            data.next = currentPage.replace(pageParam, pageParamKey + '=' + (search.page + 1))
        }

        data.items = esRes.hits.hits.collect {
            def item = [:] //iri: it.id
            //it.type (== 'doc' right now...)
            it.fields.each { key, hf ->
                // TODO: if (key.indexOf('.') > -1) { ... key.substring(0, key.indexOf('.')) }
                if (hf.value != null) {
                    item[key] = hf.values.size() > 1?  hf.values : hf.value
                }
            }
            if (item.iri) {
                item.describedby = makeServiceLink(item.iri)
            }
            it.highlightFields.each { key, hlf ->
                item.get('matches', [:])[key] = hlf.fragments
            }
            return item
        }
        return data
    }

    Map prepareElasticSearch(SearchRequestBuilder srb, Reference ref, List<String> listTerms) {
        def query = ref.getQueryAsForm(UTF_8)
        def page = 0
        def pageSize = defaultPageSize
        srb.addFields(listTerms as String[])
        def q = null
        def terms = [:]
        for (name in query.names) {
            def value = query.getFirstValue(name)
            if (name == 'q') {
                q = query.getFirstValue('q')
            } else if (name == '_sort') {
                value.split(",").each {
                    def sortTerm = it
                    def sortOrder = SortOrder.ASC
                    if (it.startsWith('-')) {
                        sortOrder = SortOrder.DESC
                        sortTerm = it.substring(1)
                    }
                    if (elasticData.termsWithRawField.contains(sortTerm)) {
                        sortTerm = sortTerm + ".raw"
                    } else if (!elasticData.dateTerms.contains(sortTerm)) {
                        // TODO: if not sortable; silent or client error?
                        return // not sortable
                    }
                    srb.addSort(sortTerm, sortOrder)
                        if (!listTerms.contains(sortTerm))
                            srb.addFields(sortTerm)
                }
            } else if (name == pageParamKey) {
                page = value as int
            } else if (name == pageSizeParamKey) {
                pageSize = value as int
            } else {
                terms[name] = query.getValuesArray(name)
            }
        }
        def matches = []
        if (q) {
            matches << q
        }
        terms.each { name, values ->
            matches << values.collect { "${name}:${it}" }.join(" ")
            if (!listTerms.contains(name))
                srb.addFields(name)
        }
        def elasticQStr = matches.collect { "(${it})" }.join(' AND ')
        log.debug "Using ElasticSearch query string: ${elasticQStr}"
        QueryBuilder qb = (matches)?
            QueryBuilders.queryString(elasticQStr) :
            QueryBuilders.matchAllQuery()
        //TermFilterBuilder fb = FilterBuilders.termFilter("longval", 124L)

        srb.setQuery(qb)
        //srb.setQuery(QueryBuilders.filteredQuery(qb, fb))
        def startIndex = page * pageSize
        srb.setFrom(startIndex)
        srb.setSize(pageSize)

        if (q) { // free text query
            srb.setHighlighterPreTags('<em class="match">')
            srb.setHighlighterPostTags('</em>')
            srb.addHighlightedField("title", 150, 0)
            srb.addHighlightedField("identifier", 150, 0)
            //srb.addHighlightedField("publisher.name", 150, 0)
            srb.addHighlightedField("content", 150, 3)
        }

        return [
            page: page,
            pageSize: pageSize,
            startIndex: startIndex,
            queryString: ref.query ?: ''
        ]
    }

    String makeServiceLink(String iri) {
        // TODO: Experimental. Use base from request? Link to alt mediaType versions?
        return iri.replaceFirst(/http:\/\/rinfo\.lagrummet\.se\/([^#]+)(#.*)?/, serviceAppBaseUrl + '$1/data.json$2')
    }

    def getElasticStats(SearchRequestBuilder srb) {
        def qb = QueryBuilders.matchAllQuery()
        srb.setQuery(qb)
        srb.addFacet(FacetBuilders.termsFacet("type").field("type"))
        elasticData.refTerms.each {
            srb.addFacet(FacetBuilders.termsFacet(it).field(it + ".iri"))
        }
        elasticData.dateTerms.each {
            srb.addFacet(FacetBuilders.dateHistogramFacet(it).
                    field(it).interval("year"))
        }
        srb.setSize(0)
        SearchResponse esRes = srb.execute().actionGet()
        def data = [
            type: "DataSet",
            //totalResults: esRes.hits.totalHits(),
            slices: esRes.facets.collect {
                [
                    dimension: it.name,
                    observations: it.entries.collect {
                        it instanceof DateHistogramFacet.Entry?
                            [year: 1900 + new Date(it.time).year, count: it.count] :
                            [term: it.term, count: it.count]
                    }
                ]
            }
        ]
        return data
    }

}
