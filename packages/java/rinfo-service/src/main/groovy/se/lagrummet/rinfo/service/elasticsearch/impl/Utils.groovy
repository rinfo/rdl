package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet

/**
 * Created by christian on 2/18/15.
 */
class Utils {
    static Map buildStats(SearchResponse esRes) {
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
            if (hf.value != null) {
                lItem[lKey] = hf.values.size() > 1?  hf.values : hf.value
            }
        }
        if (item.iri) {
            item.describedby = makeServiceLink(item.iri, iriReplaceUrl)
        }
        hit.highlightFields.each { key, hlf ->
            item.get('matches', [:])[key] = hlf.fragments.collect { it.toString() }
        }
        return item
    }

    static String makeServiceLink(String iri, String iriReplaceUrl) {
        // TODO: Experimental. Use base from request? Link to alt mediaType versions?
        return iri.replaceFirst(/http:\/\/rinfo\.lagrummet\.se\/([^#]+)(#.*)?/, iriReplaceUrl + '$1/data.json$2')
    }
}
