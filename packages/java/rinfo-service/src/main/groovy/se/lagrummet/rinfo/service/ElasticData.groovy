package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j as Log

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException


@Log
class ElasticData {

    Client client
    String indexName

    // TODO: configure from context/schema

    def termData = [
        listTerms: ["_id", "iri", "type", "title", "identifier",
            "utfardandedatum", "beslutsdatum", "issued"],
        refTerms: ["publisher", "forfattningssamling", "utrSerie",
            "rattsfallspublikation", "allmannaRadSerie"],
        dateTerms: ["utfardandedatum", "beslutsdatum", "issued"]
    ]

    def initialMappings = [
        "doc": [
            "properties": [
                "iri": ["type": "string", "index": "not_analyzed"],
                "type": ["type": "string", "index": "not_analyzed"],
                "identifier": [
                    //"type": "string", "index": "not_analyzed", "boost": 2.0
                    "type": "multi_field",
                    "fields": [
                        "identifier": ["type": "string", "index": "analyzed", "boost": 4.0],
                        "raw": ["type": "string", "index": "not_analyzed", "boost": 4.0]
                    ]
                ],
                "domsnummer": ["type": "string"]
            ]
        ]
    ]

    def listTerms = []
    def refTerms = []
    def dateTerms = []

    private def notAnalyzedFields = ["type"]
    private def termsWithRawField = ["identifier"]

    ElasticData(String host, int port, String indexName) {
        this(new TransportClient().addTransportAddress(
                new InetSocketTransportAddress(host, port)), indexName)
    }

    ElasticData(Client client, indexName) {
        this.client = client
        this.indexName = indexName
        initRefMappings()
    }

    void initRefMappings() {
        termData.refTerms.each {
            initialMappings["doc"]["properties"][it] = [
                "properties": [
                    "iri": ["type": "string", "index": "not_analyzed"]
                ]
            ]
        }
    }

    String getFieldForSort(String term) {
        if (termsWithRawField.contains(term)) {
            return term + ".raw"
        } else if (dateTerms.contains(term) ||
                notAnalyzedFields.contains(term)) {
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
        initialMappings.each { type, mappings ->
            indices.preparePutMapping(indexName).setType(type).setSource(
                (type): mappings
            ).execute().actionGet()
        }
        updateKnownTerms()
    }

    /**
     * Reads the mappings from elasticsearch and creates term lists from
     * termData by checking if terms occur in read mappings.
     */
    synchronized void updateKnownTerms() {
        listTerms = []
        refTerms = []
        dateTerms = []
        def clusterStateRequestBuilder = client.admin().cluster().
                prepareState().setFilterIndices(indexName)
        def clusterState = clusterStateRequestBuilder.execute().actionGet().state
        def indexMetaData = clusterState.metaData.index(indexName)
        indexMetaData.mappings.each { esType, mappingMetaData ->
            // TODO: Simple indexOf-hack since current ES API doesn't expose parsed JSON
            def mappingJsonRepr = mappingMetaData.source().string()
            updateTermList(mappingJsonRepr, termData.listTerms, listTerms)
            updateTermList(mappingJsonRepr, termData.refTerms, refTerms)
            updateTermList(mappingJsonRepr, termData.dateTerms, dateTerms)
        }
    }

    private void updateTermList(mappingJsonRepr, possibleTerms, terms) {
        for (term in possibleTerms) {
            if (mappingJsonRepr.indexOf('"' + term + '"') > -1) {
                terms << term
            }
        }
    }

}
