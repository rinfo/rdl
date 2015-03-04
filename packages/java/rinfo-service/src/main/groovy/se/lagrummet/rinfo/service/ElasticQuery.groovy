package se.lagrummet.rinfo.service

import groovy.transform.CompileStatic
import groovy.util.logging.Commons as Log

import static org.restlet.data.CharacterSet.UTF_8

import org.elasticsearch.action.search.SearchPhaseExecutionException
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryStringQueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet
import org.elasticsearch.search.sort.SortOrder
import org.restlet.data.Reference

@Log
class ElasticQuery {
	
	public static final String regex_sanitize_elasticsearch = "([+\\-!\\(\\){}\\[\\]\\/^\"~*?:\\\\]|[&\\|]{2})";
	public static final String replacement = "\\\\\$1";

    ElasticData elasticData
    JsonLdSettings jsonLdSettings
    String serviceAppBaseUrl

    def defaultPageSize = 50
    def sortParamKey = '_sort'
    def pageParamKey = '_page'
    def pageSizeParamKey = '_pageSize'
    def statsParamKey = '_stats'

    def segmentWildcard = "*"
    def facetStatsSubSegment = "stats"

    Map elasticfields = [:]

    def boostMap =  [:]

    ElasticQuery(ElasticData elasticData, String serviceAppBaseUrl) {
        this.elasticData = elasticData
        this.jsonLdSettings = elasticData.jsonLdSettings
        this.serviceAppBaseUrl = serviceAppBaseUrl
        this.jsonLdSettings.listFramesData.each {
            this.elasticfields += [(it.key): compress(it.value)?.keySet()]
        }
    }

    @CompileStatic
    Map search(String docType, Reference ref) {
        boolean onlyStats = false
        int splitAt = docType.indexOf(';')
        if (splitAt > -1) {
            onlyStats = docType.substring(splitAt+1) == facetStatsSubSegment
            docType = docType.substring(0, splitAt)
        }
        if (docType == segmentWildcard) {
            docType = null
        }
        def srb = newSearchRequestBuilder()
        return onlyStats?
            getElasticStats(srb, docType, ref) :
            searchElastic(srb, docType, ref)
    }

    SearchRequestBuilder newSearchRequestBuilder() {
        return elasticData.client.prepareSearch(elasticData.indexName)
    }

    //@CompileStatic
    Map getElasticStats(SearchRequestBuilder srb, String docType, Reference ref) {
        //def qb = QueryBuilders.matchAllQuery()
        //srb.setQuery(qb)
        prepareElasticSearch(srb, ref, docType, Collections.emptyList(), false, true)
        SearchResponse esRes = srb.execute().actionGet()
        return buildStats(esRes)
    }

    def isLeaf(value) {
        if(value == null
            || value instanceof String
            || value instanceof Integer) return true
        return false
    }
    //flatten the map containing all interesting properties for the query
    //needed because fields don't accept complextypes
    def compress( Map m, String prefix = '' ) {
        prefix = prefix ? "$prefix." : ''
        m.collectEntries { k, v ->
            if(!isLeaf(v)) compress( v, "$prefix$k" )
            else
                if(k == "_boost") {
                    boostMap << [(prefix[0..-2].toString()): v ]
                    [ (prefix[0..-2].toString()): v ]
                }
                else [ ("$prefix$k".toString()): v ]
        }
    }

    //@CompileStatic
    Map searchElastic(SearchRequestBuilder srb, String docType, Reference ref) {
        def showTerms = elasticfields[docType]
        if (!showTerms) {
            return null
        }

        Map prepSearch = null
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

    Map prepareElasticSearch(SearchRequestBuilder srb, Reference ref,
            String docType,
            Collection<String> showTerms,
            escapeQueryTexts=false,
            addStats=false,
            pageSize=defaultPageSize) {
        def queryForm = ref.getQueryAsForm(UTF_8)
        def terms = [:]
        def ranges = [:]
        def optionals = new HashSet()
        def orAbles = new HashSet()
        def exists = [:]
        def page = 0
        if (docType) {
            srb.setTypes(docType)
        }
        srb.addFields(showTerms as String[])
        for (queryName in queryForm.names) {
            def queryItem = toQueryItem(queryName)
            def value = queryForm.getFirstValue(queryItem.name)
            if (value == null) {
                continue
            }
            def term = queryItem.term
            if (queryItem.optOr) {
                orAbles << term
            }
            value = escapeQueryTexts? escapeQueryString(value) : value.replace(":", "\\:")
            if (queryItem.sortKey) {
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
            } else if (queryItem.pageKey) {
                page = value as int
            } else if (queryItem.pageSizeKey) {
                pageSize = value as int
            } else if (queryItem.statsKey) {
                addStats = true
            } else if (queryItem.existsKey) {
                exists[term] = (value != "false" && value != "0")
            } else {
                if (queryItem.yearKey) {
                    ranges.get(term, [:]).year = value
                } else if (queryItem.minExKey) {
                    ranges.get(term, [:]).minEx = value
                } else if (queryItem.minKey) {
                    ranges.get(term, [:]).min = value
                } else if (queryItem.maxExKey) {
                    ranges.get(term, [:]).maxEx = value
                } else if (queryItem.maxKey) {
                    ranges.get(term, [:]).max = value
                } else {
                    terms[term] = queryForm.getValuesArray(queryItem.name).collect {
                        escapeQueryString(it)
                    }
                }
                if (queryItem.optIfExists) {
                    optionals << term
                }
            }
        }
        def matches = []
        def orMatches = []
        terms.each { term, values ->
            def expression = values.collect {
                    (term == 'q')? it : "${term}:${it}"
                }.join(" OR ")
            if (term in orAbles) {
                orMatches << expression
            } else {
                matches << expression
            }
            if (!showTerms.contains(term)) srb.addFields(term)
        }
        if (orMatches) {
            matches << orMatches.collect { "(${it})" }.join(' OR ')
        }
        def elasticQStr = matches.collect { "(${it})" }.join(' AND ')

        QueryBuilder qb = (matches)?
            QueryBuilders.queryString(elasticQStr).
                defaultOperator(QueryStringQueryBuilder.Operator.AND).field("_all") :
            QueryBuilders.matchAllQuery()

        if(matches)
            qb = addBoostedFields(qb)

        List<FilterBuilder> filterBuilders = []
        List<FilterBuilder> orFilterBuilders = []

        ranges.each { term, item ->
            def rfb = FilterBuilders.rangeFilter(term)
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
            if (term in optionals) {
                rfb = FilterBuilders.orFilter(
                        FilterBuilders.notFilter(FilterBuilders.existsFilter(term)),
                        rfb)
            }
            if (term in orAbles) {
                orFilterBuilders << rfb
            } else {
                filterBuilders << rfb
            }
            if (!showTerms.contains(term)) srb.addFields(term) //term.replaceFirst(/\..+/, ".*")
        }

        exists.each { term, value ->
            def efb = FilterBuilders.existsFilter(term)
            if (!value) {
                efb = FilterBuilders.notFilter(efb)
            }
            if (term in orAbles) {
                orFilterBuilders << efb
            } else {
                filterBuilders << efb
            }
        }

        if (orFilterBuilders) {
            filterBuilders << FilterBuilders.orFilter(orFilterBuilders as FilterBuilder[])
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

        if ('q' in terms) { // free text query
            srb.setHighlighterPreTags('<em class="match">')
            srb.setHighlighterPostTags('</em>')
            // TODO: extract fields with matchable text content to config (ElasticData)
            srb.addHighlightedField("title", 150, 0)
            srb.addHighlightedField("identifier", 150, 0)
            //srb.addHighlightedField("publisher.name", 150, 0)
            srb.addHighlightedField("text", 150, 3)
            srb.addHighlightedField("referatrubrik", 150, 0)
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

    def toQueryItem(final String name_dirty) {
        String name = name_dirty.replaceAll(regex_sanitize_elasticsearch, replacement);
		def item = [
            name: name,
            term: null,
            optOr: false,
            optIfExists: false
        ]
        if (name == sortParamKey) {
            item.sortKey = true
        } else if (name == pageParamKey) {
            item.pageKey = true
        } else if (name == pageSizeParamKey) {
            item.pageSizeKey = true
        } else if (name == statsParamKey) {
            item.statsKey = true
        } else {
            def term = name
            if (term.startsWith('or-')) {
                term = term.substring(3)
                item.optOr = true
            }
            if (term.startsWith('exists-')) {
                item.existsKey = true
                term = term.substring(7)
            } else if (term.startsWith('ifExists-')) {
                term = term.substring(9)
                item.optIfExists = true
            }
            if (term.startsWith('year-')) {
                item.yearKey = true
                item.term = term.substring(5)
            } else if (term.startsWith('minEx-')) {
                item.minExKey = true
                item.term = term.substring(6)
            } else if (term.startsWith('min-')) {
                item.minKey = true
                item.term = term.substring(4)
            } else if (term.startsWith('maxEx-')) {
                item.maxExKey = true
                item.term = term.substring(6)
            } else if (term.startsWith('max-')) {
                item.maxKey = true
                item.term = term.substring(4)
            } else {
                item.term = term
            }
        }
        return item
    }

    String escapeQueryString(String qs) {
        return qs.
            replaceAll(/(?<!\\)([:&|\\()\[\]{}"])/, /\\$1/).
            replaceAll(/^(AND|OR)|(AND|OR)$/, "").
            replace("/", "\\/")
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
            item.get('matches', [:])[key] = hlf.fragments.collect { it.toString() }
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
                        def value = isDate? 1900 + new Date(it.time).year : it.term.toString()
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

    QueryStringQueryBuilder addBoostedFields(qb) {
        boostMap.each { key,boostValue ->
            qb.field("${key}",boostValue)
        }
        return qb
    }
}
