package se.lagrummet.rinfo.service

import groovy.util.logging.Commons as Log

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.indices.InvalidIndexNameException
import org.elasticsearch.transport.RemoteTransportException
import groovy.json.JsonSlurper


@Log
class ElasticData {

    Client client
    String indexName
    String ignoreMalformed

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
            ], [
                "resource_text": [
                        "match": "text",
                        "mapping": ["type": "string", "index": "analyzed", "analyzer":"swedish_with_sfs_nja_malnummer", "include_in_all": false]
                ]
            ]
        ]
    ]

    ElasticData(String host, int port, String indexName, JsonLdSettings jsonLdSettings, String ignoreMalformed) {
        this(new TransportClient().addTransportAddress(
                new InetSocketTransportAddress(host, port)), indexName, jsonLdSettings, ignoreMalformed)
    }

    ElasticData(Client client, String indexName, JsonLdSettings jsonLdSettings, String ignoreMalformed) {
        this.client = client
        this.indexName = indexName
        this.jsonLdSettings = jsonLdSettings
        this.ignoreMalformed = ignoreMalformed
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

    synchronized def readSettings() {
        return new JsonSlurper().parse(new InputStreamReader(this.getClass().getResourceAsStream("/elasticsearch_settings.json")))
    }

    synchronized void initialize() {
        def indices = client.admin().indices()
        def settings = readSettings()
        try {
            indices.prepareCreate(indexName).setSettings(settings).execute().actionGet()
        } catch (IndexAlreadyExistsException e) {
            log.info "ElasticSearch index '${indexName}' already exists."
        } catch (InvalidIndexNameException e) {
            log.info "ElasticSearch index ${indexName} already exists as an Alias"
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
                    propMap[term] = ["type": "date", "format": "dateOptionalTime", "ignore_malformed": ignoreMalformed]
                } else if (term in jsonLdSettings.plainStringTerms) {
                    // TODO: only boost on "top level" (or reduce on depth)!
                    float boost = jsonLdSettings.boostTermMap[term] ?: 1.0
                    propMap[term] = [
                        "type": "string",
                        "fields": [
                            (term): ["type": "string", "index": "analyzed", "boost": boost]
                        ]
                    ]
                } else {
                    Float boost = jsonLdSettings.boostTermMap[term]
                    if (boost) {
                        propMap[term] = ["type": "string", "index": "analyzed", "boost": boost]
                    }
                }
                if(frame[term] instanceof Map) {
                    def analyzer = frame[term]?.analyzer
                    if (analyzer) {
                        if (!propMap[term])
                            propMap[term] = ["type": "string", "index": "analyzed", "analyzer": analyzer]
                        else
                            propMap[term].put("analyzer", analyzer)
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
