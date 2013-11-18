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

    //private Map<URL, CollectorSource> otherSourcesByFeedUrl = Collections.emptyMap()
    private List<CollectorSource> otherSourcesByFeedUrl = Collections.emptyList()

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
        //return otherSourcesByFeedUrl.values()
        return otherSourcesByFeedUrl
    }

    public void setSources(Collection<CollectorSource> sources) {
        logger.trace("sources.size="+sources.size())
        this.otherSourcesByFeedUrl = new LinkedList<>(); //new HashMap<URL, CollectorSource>()
        //todo enter 14 but exit 11. Fix please
        for (source in sources) {
            if (!exists(source))
                this.otherSourcesByFeedUrl.add(source)
            //CollectorSource cs = otherSourcesByFeedUrl.put(source.currentFeed, source)
            //if (cs!=null)
            //    logger.trace("cs.currentFeed"+(cs.currentFeed.equals(source.currentFeed)?"=":"!=")+"source.currentFeed where cs.currentFeed="+cs.currentFeed+" and source.currentFeed="+source.currentFeed)
        }
        refreshSourceFeedUrls()
    }

    private boolean exists(CollectorSource collectorSource) {
        for (source in this.otherSourcesByFeedUrl)
            if (source.currentFeed.toString().equals(collectorSource.currentFeed.toString()))
                return true;
        return false;
    }

    private CollectorSource get(URL url) {
        for (source in this.otherSourcesByFeedUrl)
            if (source.currentFeed.toString().equals(url.toString()))
                return source;
        return null;
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
            def source = get(feedUrl)
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
        mergedUrls.addAll(otherSourcesByFeedUrl)
        this.sourceFeedUrls = Collections.unmodifiableList(mergedUrls)
    }

}
