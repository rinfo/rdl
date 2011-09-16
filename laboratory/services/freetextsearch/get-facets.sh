#!/bin/sh
curl -s -X POST "http://localhost:9200/rinfo/_search?size=0&pretty=true" -d '
{
  "query": { "match_all": {} },
  "facets": {
    "type": {
      "terms": { "field": "type" }
    },
    "publisher": {
      "terms": { "field": "publisher.iri" }
    },
    "forfattningssamling": {
      "terms": { "field": "forfattningssamling.iri" }
    },
    "utredningsserie": {
      "terms": { "field": "utredningsserie.iri" }
    },
    "rattsfallspublikation": {
      "terms": { "field": "rattsfallspublikation.iri" }
    },
    "allmannaRadSerie": {
      "terms": { "field": "allmannaRadSerie.iri" }
    },
    "utfardandedatum": {
      "date_histogram": { "field": "utfardandedatum", "interval": "year" }
    }
  }
}
'
