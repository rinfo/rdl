package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j as Log

import static org.restlet.data.CharacterSet.UTF_8
import org.restlet.data.Reference

import org.elasticsearch.action.search.SearchPhaseExecutionException
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.action.search.*
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet
import org.elasticsearch.search.sort.SortOrder


@Log
class ElasticQuery {

    ElasticData elasticData
    JsonLdSettings jsonLdSettings
    String serviceAppBaseUrl

    def defaultPageSize = 50
    def sortParamKey = '_sort'
    def pageParamKey = '_page'
    def pageSizeParamKey = '_pageSize'
    def statsParamKey = '_stats'
    def facetStatsSegment = "stats"

    ElasticQuery(ElasticData elasticData, String serviceAppBaseUrl) {
        this.elasticData = elasticData
        this.jsonLdSettings = elasticData.jsonLdSettings
        this.serviceAppBaseUrl = serviceAppBaseUrl
    }

    Map search(String docType, Reference ref) {
        def srb = newSearchRequestBuilder()
        return (docType == facetStatsSegment)?
            getElasticStats(srb, ref) :
            searchElastic(srb, docType, ref)
    }

    SearchRequestBuilder newSearchRequestBuilder() {
        return elasticData.client.prepareSearch(elasticData.indexName)
    }

    Map searchElastic(SearchRequestBuilder srb, String docType, Reference ref) {
        def showTerms = jsonLdSettings.listFramesData[docType]?.keySet()
        if (!showTerms) {
            return null
        }

        def prepSearch = null
        SearchResponse esRes = null
        try {
            prepSearch = prepareElasticSearch(srb, ref, docType, showTerms)
            esRes = srb.execute().actionGet()
        } catch (Exception e) {
            if (e.cause instanceof SearchPhaseExecutionException) {
                log.debug "Malformed query <${ref}>. Retrying with escaped query text..."
                srb = newSearchRequestBuilder()
                prepSearch = prepareElasticSearch(srb, ref, docType, showTerms, true)
                esRes = srb.execute().actionGet()
            } else {
                throw e
            }
        }

        assert esRes.failedShards == 0

        def data = [
            "@language": "sv",
            "@context": jsonLdSettings.ldContextPath,
            startIndex: prepSearch.startIndex,
            itemsPerPage: prepSearch.pageSize,
            totalResults: esRes.hits.totalHits(),
            duration: "PT${esRes.took.secondsFrac}S" as String,
        ]

        addPagination(docType, prepSearch, esRes.hits.hits.length, data)
        data.items = esRes.hits.hits.collect {
            buildResultItem(it)
        }
        if (prepSearch.addStats) {
            data.statistics = buildStats(esRes)
        }
        return data
    }

    def getElasticStats(SearchRequestBuilder srb, Reference ref) {
        //def qb = QueryBuilders.matchAllQuery()
        //srb.setQuery(qb)
        prepareElasticSearch(srb, ref, null, Collections.emptyList(), false, true)
        SearchResponse esRes = srb.execute().actionGet()
        return buildStats(esRes)
    }

    Map prepareElasticSearch(SearchRequestBuilder srb, Reference ref,
            String docType,
            Collection<String> showTerms,
            escapeQueryTexts=false,
            addStats=false,
            pageSize=defaultPageSize) {
        def queryForm = ref.getQueryAsForm(UTF_8)
        def q = null
        def terms = [:]
        def ranges = [:]
        def optionals = new HashSet()
        def exists = [:]
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
            value = escapeQueryTexts? escapeQueryString(value) : value.replace(":", "\\:")
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
                        log.debug "Requested sorting on term not configured for sorting: ${sortTerm}"
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
            } else if (name.startsWith('exists-')) {
                exists[name.substring(7)] = (value != "false" && value != "0")
            } else {
                def ifExists = false
                if (name.startsWith('ifExists-')) {
                    name = name.substring(9)
                    ifExists = true
                }
                if (name.startsWith('year-')) {
                    name = name.substring(5)
                    ranges.get(name, [:]).year = value
                } else if (name.startsWith('minEx-')) {
                    name = name.substring(6)
                    ranges.get(name, [:]).minEx = value
                } else if (name.startsWith('min-')) {
                    name = name.substring(4)
                    ranges.get(name, [:]).min = value
                } else if (name.startsWith('maxEx-')) {
                    name = name.substring(6)
                    ranges.get(name, [:]).maxEx = value
                } else if (name.startsWith('max-')) {
                    name = name.substring(4)
                    ranges.get(name, [:]).max = value
                } else {
                    terms[name] = queryForm.getValuesArray(name).collect {
                        escapeQueryString(it)
                    }
                }
                if (ifExists) {
                    optionals << name
                }
            }
        }
        def matches = []
        if (q) {
            matches << q
        }
        terms.each { name, values ->
            matches << values.collect { "${name}:${it}" }.join(" ")
            if (!showTerms.contains(name)) srb.addFields(name)
        }
        def elasticQStr = matches.collect { "(${it})" }.join(' AND ')

        QueryBuilder qb = (matches)?
            QueryBuilders.queryString(elasticQStr) :
            QueryBuilders.matchAllQuery()

        List<FilterBuilder> filterBuilders = []

        ranges.each { key, item ->
            def rfb = FilterBuilders.rangeFilter(key)
            if (item.year) {
                rfb.gte(item.year)
                rfb.lt(((item.year as int) + 1) as String)
            } else {
                if (item.minEx) {
                    rfb.gt(item.minEx)
                } else if (item.min) {
                    rfb.gte(item.min)
                }
                if (item.maxEx) {
                    rfb.lt(item.maxEx)
                } else if (item.max) {
                    rfb.lte(item.max)
                }
            }
            if (key in optionals) {
                rfb = FilterBuilders.orFilter(
                        FilterBuilders.notFilter(FilterBuilders.existsFilter(key)),
                        rfb)
            }
            filterBuilders << rfb
            if (!showTerms.contains(key)) srb.addFields(key) //key.replaceFirst(/\..+/, ".*")
        }

        exists.each { key, value ->
            def efb = FilterBuilders.existsFilter(key)
            if (!value) {
                efb = FilterBuilders.notFilter(efb)
            }
            filterBuilders << efb
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
            srb.addHighlightedField("text", 150, 3)
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

    String escapeQueryString(String qs) {
        return qs.
            replaceAll(/(?<!\\)([:&|\\()\[\]{}"])/, /\\$1/).
            replaceAll(/^(AND|OR)|(AND|OR)$/, "")
    }

    void prepareStats(SearchRequestBuilder srb) {
        srb.addFacet(FacetBuilders.termsFacet("type").field("type"))
        jsonLdSettings.refTerms.each {
            def key = it + ".iri"
            srb.addFacet(FacetBuilders.termsFacet(key).field(key))
        }
        jsonLdSettings.dateTerms.each {
            srb.addFacet(FacetBuilders.dateHistogramFacet(it).
                    field(it).interval("year"))
        }
    }

    def addPagination(docType, prepSearch, hitsLength, data) {
        def pageParam = pageParamKey + '=' + prepSearch.page
        def currentPage = "/-/${docType}?" +
                ((prepSearch.queryString.indexOf(pageParam) == -1)? "${pageParam}&" : "") +
                prepSearch.queryString
        if (prepSearch.page > 0) {
            data.prev = currentPage.replace(pageParam, pageParamKey + '=' + (prepSearch.page - 1))
        }
        data.current = currentPage as String
        if (hitsLength == data.itemsPerPage &&
                (data.startIndex + data.itemsPerPage) < data.totalResults) {
            data.next = currentPage.replace(pageParam, pageParamKey + '=' + (prepSearch.page + 1))
        }
    }

    Map buildResultItem(SearchHit hit) {
        def item = [:]
        hit.fields.each { key, hf ->
            def lItem = item
            def lKey = key
            def dotAt = key.indexOf('.')
            if (dotAt > -1) {
                def baseKey = key.substring(0, dotAt)
                lItem = item.get(baseKey, [:])
                lKey = key.substring(dotAt + 1)
            }
            if (hf.value != null) {
                lItem[lKey] = hf.values.size() > 1?  hf.values : hf.value
            }
        }
        if (item.iri) {
            item.describedby = makeServiceLink(item.iri)
        }
        hit.highlightFields.each { key, hlf ->
            item.get('matches', [:])[key] = hlf.fragments
        }
        return item
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
