package se.lagrummet.rinfo.service.elasticsearch.impl

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet

/**
 * Created by christian on 2/18/15.
 */
class Utils {
    static Map buildStats(SearchResponse esRes, String iriReplaceUrl) {
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

    static Map buildStats2(SearchResponse esRes, String iriReplaceUrl) {
/*
        Terms typeAgg = esRes.aggregations.get("type")
        Terms.Bucket bucket = typeAgg.buckets.get(0)
        bucket.getAggregations().get("top").getH
        TopHits topHits = bucket.getAggregations().get("top");

        esRes.aggregations.get("top")
*/

        return [
                type: "DataSet",
                slices: esRes.aggregations.collect {
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
                                        items: createListOfSearchHits2(topHits.hits, iriReplaceUrl)
                                ]
                            }
                    ]
                }.findAll {
                    it.observations
                }
        ]
    }

    /*"statistics" : {
    "type" : "DataSet",
    "slices" : [ {
      "dimension" : "type",
      "observations" : [ {
        "term" : "KonsolideradGrundforfattning",
        "count" : 9440
      }, {
        "term" : "Forordning",
        "count" : 27
      }, {
        "term" : "Rattsfallsreferat",
        "count" : 1
      } ]
    } ]
  },*/

    static List createListOfSearchHits2(SearchHits hits, String iriReplaceUrl) {
        def list = []
        for (def hit : hits) {
            def source = hit.getSource()
            list.add(source)
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

/*
// sr is here your SearchResponse object
Terms agg = sr.getAggregations().get("agg");

// For each entry
for (Terms.Bucket entry : agg.getBuckets()) {
    String key = entry.getKey();                    // bucket key
    long docCount = entry.getDocCount();            // Doc count
    logger.info("key [{}], doc_count [{}]", key, docCount);

    // We ask for top_hits for each bucket
    TopHits topHits = entry.getAggregations().get("top");
    for (SearchHit hit : topHits.getHits().getHits()) {
        logger.info(" -> id [{}], _source [{}]", hit.getId(), hit.getSourceAsString());
    }
}
*/