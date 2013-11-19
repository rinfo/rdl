package se.lagrummet.rinfo.service

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class SesameLoadScheduler extends AbstractCollectScheduler {

    ServiceComponents components
    protected Collection<URI> sourceFeedUrls = new LinkedList<>()

    SesameLoadScheduler(components, Collection<URL> sourceFeedUrls) {
        this.components = components
        for (source in sourceFeedUrls)
            this.sourceFeedUrls.add(source.toURI())
    }

    public Collection<URI> getSourceFeedUrls() {
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
