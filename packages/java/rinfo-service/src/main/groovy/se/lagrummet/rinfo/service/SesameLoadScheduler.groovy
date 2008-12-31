package se.lagrummet.rinfo.service

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class SesameLoadScheduler extends AbstractCollectScheduler {

    private Repository repository
    private Collection<URL> sourceFeedUrls

    SesameLoadScheduler(AbstractConfiguration config, Repository repository) {
        this.repository = repository
        this.configure(config)
    }

    void configure(AbstractConfiguration config) {
        setInitialDelay(-1) // TODO:? never schedule running collects?
        sourceFeedUrls = new ArrayList<URL>()
        for (String url : config.getList("rinfo.service.sourceFeedUrls")) {
            sourceFeedUrls.add(new URL(url))
        }
    }

    void shutdown() {
        super.shutdown()
        repository.shutDown()
    }

    public Collection getSourceFeedUrls() {
        return sourceFeedUrls
    }

    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        def rdfStoreLoader = new SesameLoader(repository)
        rdfStoreLoader.readFeed(feedUrl)
    }

    protected void addRepositoryListener(listener) {
        repository.addRepositoryListener(listener)
    }

    protected void addRepositoryConnectionListener(listener) {
        repository.addRepositoryConnectionListener(listener)
    }

}
