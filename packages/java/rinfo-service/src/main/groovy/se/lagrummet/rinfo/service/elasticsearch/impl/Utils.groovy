package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet
import se.lagrummet.rinfo.service.elasticsearch.ElasticSearchQueryBuilder

/**
 * Created by christian on 2/18/15.
 */
class Utils {
    static Map buildStats(SearchResponse esRes, String iriReplaceUrl) {
        return [
                type: "DataSet",
                slices: esRes.aggregations?.aggregations.collect {
                    def iriPos = it.name.indexOf(".iri")
                    def isIri = iriPos > -1
                    return [
                            dimension: isIri? it.name.substring(0, iriPos) : it.name,
                            observations: it.buckets.collect {
                                def isDate = it instanceof DateHistogramFacet.Entry
                                def key = isDate? "year" : isIri? "ref" : "term"
                                def value = isDate? 1900 + new Date(it.time).year : it.key.toString()
                                def topHits = it.getAggregations().get("top");
                                return [(
                                        key): value,
                                        count: it.docCount,
                                        items: createListOfTopSearchHits(topHits?.hits, iriReplaceUrl)
                                ]
                            }
                    ]
                }.findAll {
                    it.observations
                }
        ]
    }

    static List createListOfTopSearchHits(SearchHits hits, String iriReplaceUrl) {
        def list = []
        for (def hit : hits) {
            hit.fields = hit.getSource()
            list.add( buildResultItem(hit, iriReplaceUrl))
        }
        return list
    }

    static List createListOfSearchHits(SearchHits hits, String iriReplaceUrl) {
        def list = []
        for (def hit : hits) {
            list.add( buildResultItem(hit, iriReplaceUrl))
        }
        return list
    }

    static Map buildResultItem(SearchHit hit, String iriReplaceUrl) {
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
            if (hf.value && !(hf instanceof String) && !(hf instanceof List)) {
                lItem[lKey] = hf.values.size() > 1 ?  hf.values : hf.value
            } else {
                lItem[lKey] = hf
            }
        }
        if (item.iri) {
            item.describedby = makeServiceLink(item.iri, iriReplaceUrl)
        }
        hit.highlightFields.each { key, hlf ->
            //todo improve this solution to manually fix truncations when Elastic limit of 150 for some reason fails. see truncate(v)
            item.get('matches', [:])[key] = hlf.fragments.collect { def v = it.toString(); return v.length() > 180 ? truncate(v) : v }
        }
        item.explanation = hit.getExplanation()?.toHtml()
        return item
    }

    static def truncate(String v) {
        int startOfHighlighter = v.indexOf(ElasticSearchQueryBuilder.HIGHLIGHTERS_TAG.start)
        int endOfHighlighter = v.lastIndexOf(ElasticSearchQueryBuilder.HIGHLIGHTERS_TAG.end)

        if (startOfHighlighter==-1 || endOfHighlighter == -1)
            return v
        int calculatedLastPartBehindHighlighter = v.length() - ( endOfHighlighter + ElasticSearchQueryBuilder.HIGHLIGHTERS_TAG.end.length() )
        if (calculatedLastPartBehindHighlighter < 0)
            return v

        String start = v.substring(0,startOfHighlighter)
        String mid = v.substring(startOfHighlighter, endOfHighlighter + ElasticSearchQueryBuilder.HIGHLIGHTERS_TAG.end.length())
        String end = v.substring(endOfHighlighter + ElasticSearchQueryBuilder.HIGHLIGHTERS_TAG.end.length())

        int startLength = start.length()
        int endLength = end.length()
        if (startLength+endLength<=150)
            return v

        if (startLength > 150) startLength = 150;
        if (endLength > 150) endLength = 150;

        if (startLength>=endLength) {
            endLength = endLength / 2
            startLength -= endLength
        } else {
            startLength = startLength / 2
            endLength -= startLength
        }
        return start.substring(start.length() - startLength) + mid + end.substring(0, endLength)
    }

    static String makeServiceLink(String iri, String iriReplaceUrl) {
        // TODO: Experimental. Use base from request? Link to alt mediaType versions?
        return iri.replaceFirst(/http:\/\/rinfo\.lagrummet\.se\/([^#]+)(#.*)?/, iriReplaceUrl + '$1/data.json$2')
    }
}