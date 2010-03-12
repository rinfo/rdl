package se.lagrummet.rinfo.service

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException

import org.openrdf.repository.Repository
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class SesameLoadScheduler extends AbstractCollectScheduler {

    private NotifyingRepositoryWrapper repository
    private Collection<URL> sourceFeedUrls

    SesameLoadScheduler(Configuration config, Repository repository) {
        this.repository = new NotifyingRepositoryWrapper(repository)
        this.configure(config)
    }

    void configure(Configuration config) {
        // TODO: never schedule running collects?
        setInitialDelay(-1)
        setScheduleInterval(-1)
        sourceFeedUrls = new ArrayList<URL>()
        for (String url : config.getList("rinfo.service.sourceFeedUrls")) {
            sourceFeedUrls.add(new URL(url))
        }
    }

    public Collection getSourceFeedUrls() {
        return sourceFeedUrls
    }

    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        def rdfStoreLoader = new SesameLoader(repository)
        rdfStoreLoader.readFeed(feedUrl)
        rdfStoreLoader.shutdown()
    }

    protected void addRepositoryListener(listener) {
        repository.addRepositoryListener(listener)
    }

    protected void addRepositoryConnectionListener(listener) {
        repository.addRepositoryConnectionListener(listener)
    }

}
