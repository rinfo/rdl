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
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.FilterBuilders
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

    def jsonMapper

    def defaultPageSize = 50
    def sortParamKey = '_sort'
    def pageParamKey = '_page'
    def pageSizeParamKey = '_pageSize'
    def statsParamKey = '_stats'
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
        final String docType = request.attributes["docType"]
        SearchRequestBuilder srb = elasticData.client.prepareSearch(elasticData.indexName)
        def data = (docType == facetStatsSegment)?
            getElasticStats(srb, request.resourceRef) :
            searchElastic(srb, docType, request.resourceRef)
        return new ServerResource() {
            @Get("json")
            Representation asJSON() {
                def jsonStr = jsonMapper.writeValueAsString(data)
                def mediaType = MediaType.APPLICATION_JSON
                return new StringRepresentation(jsonStr, mediaType, null, UTF_8)
            }
        }
    }

    def searchElastic(SearchRequestBuilder srb, String docType, Reference ref) {
        // TODO:
        // - showTerms by docType
        // - 404 if docType not in known mappings
        def prepSearch = prepareElasticSearch(srb, ref, docType, elasticData.showTerms)

        SearchResponse esRes = srb.execute().actionGet()
        assert esRes.failedShards == 0

        def data = [
            "@language": "sv",
            "@context": "/json-ld/list-context.json",
            startIndex: prepSearch.startIndex,
            itemsPerPage: prepSearch.pageSize,
            totalResults: esRes.hits.totalHits(),
            duration: "PT${esRes.took.secondsFrac}S" as String,
        ]

        def pageParam = pageParamKey + '=' + prepSearch.page
        def currentPage = "/-/${docType}?" +
                ((prepSearch.queryString.indexOf(pageParam) == -1)? "${pageParam}&" : "") +
                prepSearch.queryString
        if (prepSearch.page > 0) {
            data.prev = currentPage.replace(pageParam, pageParamKey + '=' + (prepSearch.page - 1))
        }
        data.current = currentPage as String
        if (esRes.hits.hits.length == data.itemsPerPage &&
                (data.startIndex + data.itemsPerPage) < data.totalResults) {
            data.next = currentPage.replace(pageParam, pageParamKey + '=' + (prepSearch.page + 1))
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
        if (prepSearch.addStats) {
            data.statistics = buildStats(esRes)
        }
        return data
    }

    def getElasticStats(SearchRequestBuilder srb, Reference ref) {
        //def qb = QueryBuilders.matchAllQuery()
        //srb.setQuery(qb)
        prepareElasticSearch(srb, ref, null, [], 0, true)
        SearchResponse esRes = srb.execute().actionGet()
        return buildStats(esRes)
    }

    Map prepareElasticSearch(SearchRequestBuilder srb, Reference ref,
            String docType, List<String> showTerms,
            pageSize=defaultPageSize, addStats=false) {
        def queryForm = ref.getQueryAsForm(UTF_8)
        def q = null
        def terms = [:]
        def ranges = [:]
        def page = 0
        if (docType) {
            srb.setTypes(docType)
        }
        srb.addFields(showTerms as String[])
        for (name in queryForm.names) {
            def value = queryForm.getFirstValue(name)
            if (value == null) {
                continue
            }
            value = value.replace(":", "\\:")
            if (name == 'q') {
                q = value
            } else if (name == sortParamKey) {
                value?.split(",").each {
                    def sortTerm = it
                    def sortOrder = SortOrder.ASC
                    if (it.startsWith('-')) {
                        sortOrder = SortOrder.DESC
                        sortTerm = it.substring(1)
                    }
                    def field = elasticData.getFieldForSort(sortTerm)
                    if (field == null) {
                        // TODO: if not sortable; silent or client error?
                        return // not sortable
                    }
                    srb.addSort(field, sortOrder)
                    if (!showTerms.contains(sortTerm)) srb.addFields(sortTerm)
                }
            } else if (name == pageParamKey) {
                page = value as int
            } else if (name == pageSizeParamKey) {
                pageSize = value as int
            } else if (name == statsParamKey) {
                addStats = true
            } else if (name.startsWith('year-')) {
                ranges.get(name.substring(5), [:]).year = value
            } else if (name.startsWith('minEx-')) {
                ranges.get(name.substring(6), [:]).minEx = value
            } else if (name.startsWith('min-')) {
                ranges.get(name.substring(4), [:]).min = value
            } else if (name.startsWith('maxEx-')) {
                ranges.get(name.substring(6), [:]).maxEx = value
            } else if (name.startsWith('max-')) {
                ranges.get(name.substring(4), [:]).max = value
            } else {
                terms[name] = queryForm.getValuesArray(name).collect {
                    it.replace(":", "\\:")
                }
            }
        }
        def matches = []
        if (q) {
            matches << q
        }
        terms.each { name, values ->
            matches << values.collect { "${name}:${it}" }.join(" ")
            if (!showTerms.contains(name)) {
                srb.addFields(name)
            }
        }
        def elasticQStr = matches.collect { "(${it})" }.join(' AND ')

        QueryBuilder qb = (matches)?
            QueryBuilders.queryString(elasticQStr) :
            QueryBuilders.matchAllQuery()

        List<FilterBuilder> filterBuilders = []
        ranges.each { key, item ->
            def rqb = FilterBuilders.rangeFilter(key)
            if (item.year) {
                rqb.gte(item.year)
                rqb.lt(((item.year as int) + 1) as String)
            } else {
                if (item.minEx) {
                    rqb.gt(item.minEx)
                } else if (item.min) {
                    rqb.gte(item.min)
                }
                if (item.maxEx) {
                    rqb.lt(item.maxEx)
                } else if (item.max) {
                    rqb.lte(item.max)
                }
            }
            filterBuilders << rqb
        }

        for (fb in filterBuilders) {
            qb = QueryBuilders.filteredQuery(qb, fb)
        }

        srb.setQuery(qb)
        def startIndex = page * pageSize
        srb.setFrom(startIndex)
        srb.setSize(pageSize)

        if (addStats) {
            prepareStats(srb)
        }

        if (q) { // free text query
            srb.setHighlighterPreTags('<em class="match">')
            srb.setHighlighterPostTags('</em>')
            // TODO: extract fields with matchable text content to config (ElasticData)
            srb.addHighlightedField("title", 150, 0)
            srb.addHighlightedField("identifier", 150, 0)
            //srb.addHighlightedField("publisher.name", 150, 0)
            srb.addHighlightedField("content", 150, 3)
        }

        log.debug "Using ElasticSearch search JSON: ${srb.internalBuilder()}"

        return [
            page: page,
            pageSize: pageSize,
            startIndex: startIndex,
            queryString: ref.query ?: '',
            addStats: addStats
        ]
    }

    def prepareStats(SearchRequestBuilder srb) {
        srb.addFacet(FacetBuilders.termsFacet("type").field("type"))
        elasticData.refTerms.each {
            def key = it + ".iri"
            srb.addFacet(FacetBuilders.termsFacet(key).field(key))
        }
        elasticData.dateTerms.each {
            srb.addFacet(FacetBuilders.dateHistogramFacet(it).
                    field(it).interval("year"))
        }
    }

    Map buildStats(SearchResponse esRes) {
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
                        def value = isDate? 1900 + new Date(it.time).year : it.term
                        return [(key): value, count: it.count]
                    }
                ]
            }.findAll {
                it.observations
            }
        ]
    }

    String makeServiceLink(String iri) {
        // TODO: Experimental. Use base from request? Link to alt mediaType versions?
        return iri.replaceFirst(/http:\/\/rinfo\.lagrummet\.se\/([^#]+)(#.*)?/, serviceAppBaseUrl + '$1/data.json$2')
    }

}
