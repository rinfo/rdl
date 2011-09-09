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
import org.elasticsearch.index.query.xcontent.*//QueryBuilders,...
import org.elasticsearch.search.sort.SortOrder

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig


class ElasticFinder extends Finder {

    def client
    def indexName
    def contextMap
    def jsonMapper

    def defaultPageSize = 50
    def pageParamKey = '_page'
    def pageSizeParamKey = '_pageSize'

    ElasticFinder(Context context, client, indexName) {
        super(context)
        this.client = client
        this.indexName = indexName
        jsonMapper = new ObjectMapper()
        jsonMapper.configure(
                SerializationConfig.Feature.INDENT_OUTPUT, true)
    }

    @Override
    ServerResource find(Request request, Response response) {
        final String collection = request.attributes["collection"]

        // TODO: if no query params: list facets with stats and context
        def search = makeElasticSearch(request.resourceRef)

        SearchResponse esRes = search.searchRequestBuilder.execute().actionGet()
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
            def item = [
                iri: it.id
            ]
            //it.type (== 'doc' right now...)
            it.fields.each { key, hf ->
                if (key == "_source.type")
                    item.type = hf.value.term
                else
                    item[key] = hf.value/*s*/
            }
            return item
        }

        return new ServerResource() {
            @Get("json")
            Representation asJSON() {
                def jsonStr = jsonMapper.writeValueAsString(data)
                def mediaType = MediaType.APPLICATION_JSON
                return new StringRepresentation(jsonStr, mediaType, null, UTF_8)
            }
        }
    }

    def makeElasticSearch(Reference ref) {
        def query = ref.getQueryAsForm(UTF_8)
        def page = 0
        def pageSize = defaultPageSize
        SearchRequestBuilder srb = client.prepareSearch(indexName)
        def matches = []
        for (name in query.names) {
            def value = query.getFirstValue(name)
            if (name == 'q') {
                matches << query.getFirstValue('q')
            } else if (name == '_sort') {
                value.split(",").collect {
                    if (it.startsWith('-')) {
                        srb.addSort(it.substring(1), SortOrder.DESC)
                    } else {
                        srb.addSort(it, SortOrder.ASC)
                    }
                }
            } else if (name == pageParamKey) {
                page = value as int
            } else if (name == pageSizeParamKey) {
                pageSize = value as int
            } else {
                matches << "${name}:${value}"
            }
        }
        XContentQueryBuilder qb = (matches)?
            QueryBuilders.queryString(matches.join(' AND ')) :
            QueryBuilders.matchAllQuery()
        //TermFilterBuilder fb = FilterBuilders.termFilter("longval", 124L)

        srb.addFields("_id", "_source.type", "iri", "type", "title", "identifier", "utfardandedatum", "beslutsdatum", "issued")
        srb.setQuery(qb)
        //srb.setQuery(QueryBuilders.filteredQuery(qb, fb))
        def startIndex = page * pageSize
        srb.setFrom(startIndex)
        srb.setSize(pageSize)
        return [
            searchRequestBuilder: srb,
            page: page,
            pageSize: pageSize,
            startIndex: startIndex,
            queryString: ref.query ?: ''
        ]
    }

}
