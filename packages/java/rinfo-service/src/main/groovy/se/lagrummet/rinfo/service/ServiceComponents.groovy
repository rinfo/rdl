package se.lagrummet.rinfo.service

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.PropertiesConfiguration


import org.codehaus.jackson.map.ObjectMapper

import org.openrdf.repository.Repository
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper

import se.lagrummet.rinfo.rdf.repo.RepositoryHandler
import se.lagrummet.rinfo.rdf.repo.RepositoryHandlerFactory

import se.lagrummet.rinfo.base.rdf.jsonld.JSONLDContext


class ServiceComponents {

    Configuration config

    private NotifyingRepositoryWrapper repository
    SesameLoadScheduler loadScheduler
    RepositoryHandler repositoryHandler
    JsonLdSettings jsonLdSettings
    ElasticData elasticData
    ElasticQuery elasticQuery

    String ldContextPath = "/json-ld/context.json"
    protected String listFramesPath = "/json-ld/list-frames.json"

    protected def mapper = new ObjectMapper()

    public Repository getRepository() {
        return repository
    }

    ServiceComponents(String configPath) {
        this(new PropertiesConfiguration(configPath))
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

    String getVarnishUrl() {
        def host = config.getString("rinfo.service.varnish.host")
        def port = config.getString("rinfo.service.varnish.port")
        return "http://" + host + ":" + port
    }

    boolean getVarnishInvalidationEnabled() {
        return Boolean.parseBoolean(config.getString("rinfo.service.varnish.invalidationEnabled"))
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
        def varnishInvalidator = new VarnishInvalidator(getVarnishUrl(), getVarnishInvalidationEnabled())
        def sourceFeedUrls = new ArrayList<URL>()
        for (url in config.getList("rinfo.service.sourceFeedUrls")) {
            sourceFeedUrls.add(new URL(url))
        }
        def loadScheduler = new SesameLoadScheduler(this, varnishInvalidator, sourceFeedUrls)
        loadScheduler.setInitialDelay(-1)
        loadScheduler.setScheduleInterval(-1)
        return loadScheduler
    }

    private def createJsonLdSettings() {
        def ldContext = new JSONLDContext(readJson(ldContextPath))
        def listFramesData = readJson(listFramesPath)
        return new JsonLdSettings(ldContext, listFramesData, ldContextPath)
    }

    private def createElasticData() {
        def esConf = config.subset("rinfo.service.elasticdata")
        if (esConf.isEmpty()) {
            return null
        }
        def host = esConf.getString("host")
        def port = esConf.getInt("port")
        def indexName = esConf.getString("index")
        def ignoreMalformed = esConf.getString("ignore_malformed")
        return new ElasticData(host, port, indexName, jsonLdSettings, ignoreMalformed)
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

    protected Map readJson(String dataPath) {
        def inStream = getClass().getResourceAsStream(dataPath)
        try {
            return mapper.readValue(inStream, Map)
        } finally {
            inStream.close()
        }
    }

}
