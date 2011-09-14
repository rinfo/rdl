package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j

import org.apache.commons.configuration.Configuration

import org.openrdf.repository.Repository
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper

import se.lagrummet.rinfo.rdf.repo.RepositoryHandler
import se.lagrummet.rinfo.rdf.repo.RepositoryHandlerFactory

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException


@Slf4j
class ServiceComponents {

    static final String REPO_PROPERTIES_SUBSET_KEY = "rinfo.service.repo"

    private NotifyingRepositoryWrapper repository
    SesameLoadScheduler loadScheduler
    RepositoryHandler repositoryHandler
    Configuration config

    Client searchClient
    def searchIndexName = "rinfo"

    public Repository getRepository() {
        return repository
    }

    ServiceComponents(Configuration config) {
        this.config = config
        repositoryHandler = RepositoryHandlerFactory.create(config.subset(
                REPO_PROPERTIES_SUBSET_KEY))
        repositoryHandler.initialize()
        this.repository = new NotifyingRepositoryWrapper(repositoryHandler.repository)
        this.loadScheduler = createLoadScheduler()
        this.searchClient = createElasticSearchClient()
    }

    String getDataAppBaseUri() {
        return config.getString("rinfo.service.dataAppBaseUri")
    }

    def newSesameLoader() {
        return new SesameLoader(repository, createElasticLoader())
    }

    void startup() {
        configureElasticSearch()
        loadScheduler.startup()
    }

    void shutdown() {
        try {
            loadScheduler.shutdown()
        } finally {
            repositoryHandler.shutDown()
        }
    }

    protected void addRepositoryListener(listener) {
        repository.addRepositoryListener(listener)
    }

    protected void addRepositoryConnectionListener(listener) {
        repository.addRepositoryConnectionListener(listener)
    }

    private def createLoadScheduler() {
        // TODO: never schedule running collects?
        def sourceFeedUrls = new ArrayList<URL>()
        for (url in config.getList("rinfo.service.sourceFeedUrls")) {
            sourceFeedUrls.add(new URL(url))
        }
        def loadScheduler = new SesameLoadScheduler(this, sourceFeedUrls)
        loadScheduler.setInitialDelay(-1)
        loadScheduler.setScheduleInterval(-1)
        return loadScheduler
    }

    private def createElasticSearchClient() {
        // TODO: move to config file; return null if config for this is missing
        def host = "127.0.0.1"
        def port = 9300
        return new TransportClient().addTransportAddress(
                new InetSocketTransportAddress(host, port))
    }

    private void configureElasticSearch() {
        def indices = searchClient.admin().indices()
        try {
            indices.prepareCreate(searchIndexName).execute().actionGet()
        } catch (IndexAlreadyExistsException e) {
            log.info "ElasticSearch index '${searchIndexName}' already exists."
        } catch (RemoteTransportException e) {
            if (e.cause instanceof IndexAlreadyExistsException) {
                log.info "ElasticSearch index '${searchIndexName}' already exists."
            } else {
                throw e
            }
        }
        // TODO: configure from context/schema
        indices.preparePutMapping(searchIndexName).setType("doc").setSource([
            "doc": [
                "properties": [
                    "domsnummer": ["type": "string"]
                ]
            ]
        ]).execute().actionGet()
    }

    private def createElasticLoader() {
        if (searchClient == null) {
            return null
        }
        return new ElasticLoader(searchClient, searchIndexName)
    }

}
