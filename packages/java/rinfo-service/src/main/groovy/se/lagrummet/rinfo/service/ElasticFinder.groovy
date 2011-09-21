package se.lagrummet.rinfo.service

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


class ElasticFinder extends Finder {

    ElasticData elasticData
    def contextMap
    def jsonMapper

    def defaultPageSize = 50
    def pageParamKey = '_page'
    def pageSizeParamKey = '_pageSize'
    def facetStatsSegment = "stats"

    ElasticFinder(Context context, ElasticData elasticData) {
        super(context)
        this.elasticData = elasticData
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
        if (esRes.hits.hits.length == search.pageSize) {
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
            return item
        }
        return data
    }

    Map prepareElasticSearch(SearchRequestBuilder srb, Reference ref, List<String> listTerms) {
        def query = ref.getQueryAsForm(UTF_8)
        def page = 0
        def pageSize = defaultPageSize
        srb.addFields(listTerms as String[])
        def matches = []
        for (name in query.names) {
            def value = query.getFirstValue(name)
            if (name == 'q') {
                matches << query.getFirstValue('q')
            } else if (name == '_sort') {
                value.split(",").each {
                    if (it.startsWith('-')) {
                        def sortKey = it.substring(1)
                        srb.addSort(sortKey, SortOrder.DESC)
                        if (!listTerms.contains(sortKey))
                            srb.addFields(sortKey)
                    } else {
                        srb.addSort(it, SortOrder.ASC)
                        if (!listTerms.contains(it))
                            srb.addFields(it)
                    }
                }
            } else if (name == pageParamKey) {
                page = value as int
            } else if (name == pageSizeParamKey) {
                pageSize = value as int
            } else {
                matches << "${name}:${value}"
                if (!listTerms.contains(name))
                    srb.addFields(name)
            }
        }
        QueryBuilder qb = (matches)?
            QueryBuilders.queryString(matches.join(' AND ')) :
            QueryBuilders.matchAllQuery()
        //TermFilterBuilder fb = FilterBuilders.termFilter("longval", 124L)

        srb.setQuery(qb)
        //srb.setQuery(QueryBuilders.filteredQuery(qb, fb))
        def startIndex = page * pageSize
        srb.setFrom(startIndex)
        srb.setSize(pageSize)
        return [
            page: page,
            pageSize: pageSize,
            startIndex: startIndex,
            queryString: ref.query ?: ''
        ]
    }

    String makeServiceLink(String iri) {
        // TODO: Experimental. Use base from request? Link to alt mediaType versions?
        return iri.replaceFirst(/http:\/\/rinfo\.([^#]+)(#.*)?/, 'http://service.$1/data.json$2')
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
