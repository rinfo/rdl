package se.lagrummet.rinfo.main

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.store.depot.FileDepot

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.rdf.RDFUtil


class Storage {

    FileDepot depot
    URIMinter uriMinter // TODO: a set of resource/rdf validators..

    private Repository registryRepo // TODO: eventRegistry?

    Storage(Configuration config) {
        configure(config)
    }

    void configure(Configuration config) {
        depot = FileDepot.newConfigured(config)
        configureValidators(config)
        configureRegistry(config)
    }

    void configureValidators(Configuration config) {
        def baseDir = config.getString("rinfo.main.baseDir")
        def repo = RDFUtil.slurpRdf(baseDir+"/datasets/containers.n3")
        def minterDir = baseDir+"/uri_algorithm"
        uriMinter = new URIMinter(repo,
                minterDir+"/collect-uri-data.rq", minterDir+"/create-uri.xslt")
    }

    void configureRegistry(Configuration config) {
        def dataDirPath = config.getString("rinfo.main.collector.registryDataDir")
        def dataDir = new File(dataDirPath)
        if (!dataDir.exists()) {
            dataDir.mkdir()
        }
        registryRepo = new SailRepository(new NativeStore(dataDir))
        registryRepo.initialize()
    }

    FeedCollectorRegistry newFeedCollectorRegistry() {
        return new FeedCollectorRegistry(registryRepo)
    }

    void shutdown() {
        registryRepo.shutDown()
    }

}
