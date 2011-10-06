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

    //def docTypes = ["publ", "org", "serie", "ns", "sys", "ext/celex", "..."]

    // TODO: showTerms and refTerms are per rootType; get dateTerms from JSON-LD context
    def termData = [
        showTerms: ["_id", "iri", "type", "title", "identifier",
            "utfardandedatum", "beslutsdatum", "issued"],
        refTerms: ["publisher", "forfattningssamling", "utrSerie",
            "rattsfallspublikation", "allmannaRadSerie"],
        dateTerms: ["utfardandedatum", "beslutsdatum", "utkomFranTryck", "ikrafttradandedatum",
                    "avkravtAvrapporteringsdatum", "avgorandedatum", "ratificieringsdatum",
                    "issued", "created", "updated"]
    ]

    // TODO: is it possible to set these for all doc types? (key "_all" doesn't work..)
    def sharedMappings = [
        "dynamic_templates": [
            [
                "resource_iri": [
                    "match": "iri",
                    "mapping": ["type": "string", "index": "not_analyzed"]
                ],
            ], [
                "resource_type": [
                    "match": "type",
                    "mapping": ["type": "string", "index": "not_analyzed"]
                ]
            ]
        ]
    ]

    def initialMappings = [

        "publ": sharedMappings + [
            //"date_detection" : false, TODO: use and do each dateTerm "type": "dateOptionalTime"
            "properties": [
                "identifier": [
                    "type": "multi_field",
                    "fields": [
                        "identifier": ["type": "string", "index": "analyzed", "boost": 4.0],
                        "raw": ["type": "string", "index": "not_analyzed", "boost": 4.0]
                    ]
                ],
                "domsnummer": ["type": "string"]
            ]
        ],

        "ns": sharedMappings + [:],

        "ext": sharedMappings + [:],

        "sys": sharedMappings + [:],

    ]

    def showTerms = []
    def refTerms = []
    def dateTerms = []

    private def notAnalyzedFields = ["iri", "type"]
    private def termsWithRawField = ["identifier"] // TODO: + "arsutgava", "lopnummer" (add as multi_field)

    ElasticData(String host, int port, String indexName) {
        this(new TransportClient().addTransportAddress(
                new InetSocketTransportAddress(host, port)), indexName)
    }

    ElasticData(Client client, indexName) {
        this.client = client
        this.indexName = indexName
    }

    String getFieldForSort(String term) {
        if (termsWithRawField.contains(term)) {
            return term + ".raw"
        } else if (dateTerms.contains(term) ||
                notAnalyzedFields.contains(term) ||
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
        showTerms = []
        refTerms = []
        dateTerms = []
        def clusterStateRequestBuilder = client.admin().cluster().
                prepareState().setFilterIndices(indexName)
        def clusterState = clusterStateRequestBuilder.execute().actionGet().state
        def indexMetaData = clusterState.metaData.index(indexName)
        indexMetaData.mappings.each { esType, mappingMetaData ->
            // TODO: Simple indexOf-hack since current ES API doesn't expose parsed JSON
            def mappingJsonRepr = mappingMetaData.source().string()
            updateTermList(mappingJsonRepr, termData.showTerms, showTerms)
            updateTermList(mappingJsonRepr, termData.refTerms, refTerms)
            updateTermList(mappingJsonRepr, termData.dateTerms, dateTerms)
        }
    }

    private void updateTermList(mappingJsonRepr, possibleTerms, terms) {
        for (term in possibleTerms) {
            if (mappingJsonRepr.indexOf('"' + term + '"') > -1) {
                if (!terms.contains(term)) {
                    terms << term
                }
            }
        }
    }

}
