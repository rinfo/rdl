package se.lagrummet.rinfo.service

import groovy.util.logging.Commons as Log

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException


@Log
class ElasticData {

    Client client
    String indexName

    JsonLdSettings jsonLdSettings

    def sharedMappings = [
        "date_detection" : false,
        "dynamic_templates": [
            [
                "resource_iri": [
                    "match": "iri",
                    "mapping": ["type": "string", "index": "not_analyzed", "include_in_all": false]
                ],
            ], [
                "resource_type": [
                    "match": "type",
                    "mapping": ["type": "string", "index": "not_analyzed", "include_in_all": false]
                ]
            ]
        ]
    ]

    ElasticData(String host, int port, String indexName, JsonLdSettings jsonLdSettings) {
        this(new TransportClient().addTransportAddress(
                new InetSocketTransportAddress(host, port)), indexName, jsonLdSettings)
    }

    ElasticData(Client client, String indexName, JsonLdSettings jsonLdSettings) {
        this.client = client
        this.indexName = indexName
        this.jsonLdSettings = jsonLdSettings
    }

    String getFieldForSort(String term) {
        if (jsonLdSettings.plainStringTerms.contains(term)) {
            return term + ".raw"
        } else if (jsonLdSettings.dateTerms.contains(term) ||
                jsonLdSettings.keywordTerms.contains(term) ||
                term.endsWith(".iri")) {
            return term
        }
        return null
    }

    synchronized void initialize() {
        def indices = client.admin().indices()
        try {
            indices.prepareCreate(indexName).execute().actionGet()
        } catch (IndexAlreadyExistsException e) {
            log.info "ElasticSearch index '${indexName}' already exists."
        } catch (RemoteTransportException e) {
            if (e.cause instanceof IndexAlreadyExistsException) {
                log.info "ElasticSearch index '${indexName}' already exists."
            } else {
                throw e
            }
        }
        jsonLdSettings.listFramesData.each { docType, frame ->
            def propMap = [:]
            for (term in frame.keySet()) {
                if (term in jsonLdSettings.refTerms) {
                } else if (term in jsonLdSettings.dateTerms) {
                    propMap[term] = ["type": "date", "format": "dateOptionalTime"]
                } else if (term in jsonLdSettings.plainStringTerms) {
                    // TODO: only boost on "top level" (or reduce on depth)!
                    float boost = jsonLdSettings.boostTermMap[term] ?: 1.0
                    propMap[term] = [
                        "type": "multi_field",
                        "fields": [
                            (term): ["type": "string", "index": "analyzed", "boost": boost],
                            "raw": ["type": "string", "index": "not_analyzed", "include_in_all": true]//, "boost": boost]
                        ]
                    ]
                } else {
                    Float boost = jsonLdSettings.boostTermMap[term]
                    if (boost) {
                        propMap[term] = ["type": "string", "index": "analyzed", "boost": boost]
                    }
                }
            }

            def mappingData = [
                date_detection: sharedMappings.date_detection,
                dynamic_templates: sharedMappings.dynamic_templates.clone()
            ]
            // Create dynamic templates for each known term to ensured indexing
            // of nested items.
            propMap.each { term, mapping ->
                mappingData.dynamic_templates << [
                    (docType + "_" + term): [
                        match: term,
                        mapping: mapping
                    ]
                ]
            }
            // We need explicit mappings as well to ensure that we can search
            // for terms not yet used in instance data (e.g. for the facets
            // view)
            mappingData["properties"] = propMap

            indices.preparePutMapping(indexName).setType(docType).setSource(
                (docType): mappingData
            ).execute().actionGet()
        }
    }

    synchronized void shutdown() {
        client.close()
    }

}
