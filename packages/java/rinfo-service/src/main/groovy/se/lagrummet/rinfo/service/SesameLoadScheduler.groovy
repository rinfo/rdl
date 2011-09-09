package se.lagrummet.rinfo.service

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class SesameLoadScheduler extends AbstractCollectScheduler {

    ServiceComponents components
    protected Collection<URL> sourceFeedUrls

    SesameLoadScheduler(components, sourceFeedUrls) {
        this.components = components
        this.sourceFeedUrls = sourceFeedUrls
    }

    public Collection<URL> getSourceFeedUrls() {
        return sourceFeedUrls
    }

    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        def repoLoader = components.newSesameLoader()
        try {
          repoLoader.readFeed(feedUrl)
        } finally {
          repoLoader.shutdown()
        }
    }

}
