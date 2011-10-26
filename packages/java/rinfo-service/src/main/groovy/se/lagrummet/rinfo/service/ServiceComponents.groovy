package se.lagrummet.rinfo.service

import org.apache.commons.configuration.Configuration

import org.openrdf.repository.Repository
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper

import se.lagrummet.rinfo.rdf.repo.RepositoryHandler
import se.lagrummet.rinfo.rdf.repo.RepositoryHandlerFactory


class ServiceComponents {

    Configuration config

    private NotifyingRepositoryWrapper repository
    SesameLoadScheduler loadScheduler
    RepositoryHandler repositoryHandler
    JsonLdSettings jsonLdSettings
    ElasticData elasticData
    ElasticQuery elasticQuery

    public Repository getRepository() {
        return repository
    }

    ServiceComponents(Configuration config) {
        this.config = config
        repositoryHandler = RepositoryHandlerFactory.create(config.subset("rinfo.service.repo"))
        repositoryHandler.initialize()
        this.repository = new NotifyingRepositoryWrapper(repositoryHandler.repository)
        this.loadScheduler = createLoadScheduler()
        this.jsonLdSettings = createJsonLdSettings()
        this.elasticData = createElasticData()
        this.elasticQuery = createElasticQuery()
    }

    String getDataAppBaseUri() {
        return config.getString("rinfo.service.dataAppBaseUri")
    }

    String getServiceAppBaseUrl() {
        return config.getString("rinfo.service.serviceAppBaseUrl")
    }

    def newSesameLoader() {
        return new SesameLoader(repository, createElasticLoader())
    }

    void startup() {
        elasticData?.initialize()
        loadScheduler.startup()
    }

    void shutdown() {
        try {
            loadScheduler.shutdown()
        } finally {
            try {
                repositoryHandler.shutDown()
            } finally {
                elasticData?.shutdown()
            }
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

    private def createJsonLdSettings() {
        return new JsonLdSettings()
    }

    private def createElasticData() {
        def esConf = config.subset("rinfo.service.elasticdata")
        if (esConf.isEmpty()) {
            return null
        }
        def host = esConf.getString("host")
        def port = esConf.getInt("port")
        def indexName = esConf.getString("index")
        return new ElasticData(host, port, indexName, jsonLdSettings)
    }

    private def createElasticLoader() {
        if (elasticData == null) {
            return null
        }
        return new ElasticLoader(elasticData)
    }

    private def createElasticQuery() {
        if (elasticData == null) {
            return null
        }
        return new ElasticQuery(elasticData, getServiceAppBaseUrl())
    }

}
