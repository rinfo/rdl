package se.lagrummet.rinfo.main.storage

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class FeedCollectScheduler extends AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(FeedCollectScheduler.class)

    private URL adminFeedUrl
    private Collection<URL> publicSourceFeedUrls

    private Collection<URL> sourceFeedUrls

    Runnable batchCompletedCallback

    private Storage storage

    FeedCollectScheduler(Storage storage) {
        this.storage = storage
        this.sourceFeedUrls = Collections.emptyList()
    }

    @Override
    public Collection<URL> getSourceFeedUrls() {
        return sourceFeedUrls
    }

    public URL getAdminFeedUrl() {
        return adminFeedUrl
    }

    public void setAdminFeedUrl(URL adminFeedUrl) {
        this.adminFeedUrl = adminFeedUrl
        newSourceFeedUrls()
    }

    public Collection<URL> getPublicSourceFeedUrls() {
        return publicSourceFeedUrls
    }

    public void setPublicSourceFeedUrls(Collection<URL> publicSourceFeedUrls) {
        this.publicSourceFeedUrls = publicSourceFeedUrls
        newSourceFeedUrls()
    }

    @Override
    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        def credentials = newStorageCredentials(feedUrl)
        def session = storage.openSession(credentials)
        FeedCollector.readFeed(session, feedUrl)
        if (batchCompletedCallback != null) {
            batchCompletedCallback.run()
        }
    }

    protected StorageCredentials newStorageCredentials(URL feedUrl) {
        return new StorageCredentials(feedUrl.equals(adminFeedUrl))
    }

    private void newSourceFeedUrls() {
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
        Collection<URL> mergedSources = new ArrayList<URL>()
        if (adminFeedUrl != null) {
            mergedSources.add(adminFeedUrl)
        }
        if (publicSourceFeedUrls != null) {
            mergedSources.addAll(publicSourceFeedUrls)
        }
        this.sourceFeedUrls = Collections.unmodifiableList(mergedSources)
    }

}
