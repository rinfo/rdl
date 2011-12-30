package se.lagrummet.rinfo.main.storage

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class FeedCollectScheduler extends AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(FeedCollectScheduler.class)

    private URI adminFeedId
    private URL adminFeedUrl

    private Map<URL, CollectorSource> otherSourcesByFeedUrl = Collections.emptyMap()

    private Collection<URL> sourceFeedUrls = Collections.emptyList()

    Runnable batchCompletedCallback

    private FeedCollector feedCollector

    FeedCollectScheduler(FeedCollector feedCollector) {
        this.feedCollector = feedCollector
    }

    @Override
    public Collection<URL> getSourceFeedUrls() { return sourceFeedUrls }

    public URI getAdminFeedId() { return adminFeedId }

    public void setAdminFeedId(URI adminFeedId) {
        this.adminFeedId = adminFeedId
    }

    public URL getAdminFeedUrl() { return adminFeedUrl }

    public void setAdminFeedUrl(URL adminFeedUrl) {
        this.adminFeedUrl = adminFeedUrl
        refreshSourceFeedUrls()
    }

    public Collection<CollectorSource> getSources() {
        return otherSourcesByFeedUrl.values()
    }

    public void setSources(Collection<CollectorSource> sources) {
        this.otherSourcesByFeedUrl = new HashMap<URL, CollectorSource>()
        for (source in sources) {
            otherSourcesByFeedUrl.put(source.currentFeed, source)
        }
        refreshSourceFeedUrls()
    }

    @Override
    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        def credentials = getStorageCredentials(feedUrl)
        feedCollector.readFeed(feedUrl, credentials)
        if (batchCompletedCallback != null) {
            batchCompletedCallback.run()
        }
    }

    protected StorageCredentials getStorageCredentials(URL feedUrl) {
        if (feedUrl.equals(adminFeedUrl)) {
            return new StorageCredentials(
                    new CollectorSource(adminFeedId, adminFeedUrl), true)
        } else {
            def source = otherSourcesByFeedUrl.get(feedUrl)
            if (source == null) {
                return null
            }
            return new StorageCredentials(source, false)
        }
    }

    private void refreshSourceFeedUrls() {
        boolean wasStarted = isStarted()
        if (wasStarted) {
            shutdown()
        }
        updateSourceFeedUrls()
        if (wasStarted) {
            startup()
        }
    }

    private void updateSourceFeedUrls() {
        Collection<URL> mergedUrls = new ArrayList<URL>()
        if (adminFeedUrl != null) {
            mergedUrls.add(adminFeedUrl)
        }
        mergedUrls.addAll(otherSourcesByFeedUrl.keySet())
        this.sourceFeedUrls = Collections.unmodifiableList(mergedUrls)
    }

}
