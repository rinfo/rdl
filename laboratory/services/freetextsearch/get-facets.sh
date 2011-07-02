#!/bin/sh
curl -s -X POST "http://localhost:9200/rinfo/doc/_search?size=0&pretty=true" -d '
{
  "query": { "match_all": {} },
  "facets": {
    "type": {
      "terms": { "field": "type.term" }
    },
    "publisher": {
      "terms": { "field": "publisher.term" }
    },
    "forfattningssamling": {
      "terms": { "field": "forfattningssamling.term" }
    },
    "utredningsserie": {
      "terms": { "field": "utredningsserie.term" }
    },
    "rattsfallspublikation": {
      "terms": { "field": "rattsfallspublikation.term" }
    },
    "allmannaRadSerie": {
      "terms": { "field": "allmannaRadSerie.term" }
    }
  }
}
'
