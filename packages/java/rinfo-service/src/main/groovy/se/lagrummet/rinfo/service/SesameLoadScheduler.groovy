package se.lagrummet.rinfo.service

import se.lagrummet.rinfo.collector.AbstractCollectScheduler
import se.lagrummet.rinfo.base.FeedUpdatePingNotifyer

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SesameLoadScheduler extends AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(SesameLoadScheduler.class)

    ServiceComponents components
    VarnishInvalidator varnishInvalidator
    protected Collection<URI> sourceFeedUrls = new LinkedList<>()
    protected Collection<URL> onCompletePingTargets
    def updatedEntries = []
    private URL publicSubscriptionFeed

    SesameLoadScheduler(components, varnishInvalidator, Collection<URL> sourceFeedUrls, Collection<URL> onCompletePingTargets, URL publicSubscriptionFeed) {
        this.publicSubscriptionFeed = publicSubscriptionFeed
        this.components = components
        this.varnishInvalidator = varnishInvalidator
        this.onCompletePingTargets = onCompletePingTargets
        logger.info("Found ${onCompletePingTargets.size()} onCompletePingTarget(s)")
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
          this.updatedEntries.addAll(repoLoader.updatedEntries)
        }
    }

    protected void afterCompletedCollect(String feedUrlStr) {
        logger.info("Completed collect of <"+ feedUrlStr +">.")

        updatedEntries.each {
            varnishInvalidator.ban(it.getPath())
        }
        if (onCompletePingTargets && !onCompletePingTargets.isEmpty() && !updatedEntries.isEmpty())
        //if (onCompletePingTargets && !onCompletePingTargets.isEmpty())
            try {
                logger.info("Notify onCompletePingTargets ${updatedEntries.size()} updated entries.")
                new Thread(new FeedUpdatePingNotifyer(null, onCompletePingTargets)).start()
            } catch (Exception e) { e.printStackTrace(); }

        updatedEntries = []
        /**
         * TODO: Improve varnish cache invalidation!
         * Ideally the varnish cache should be invalidated with precision
         * given that (or when) we know exactly which objects to remove
         *
         * To remove a specific object, use purge:
         * varnishInvalidator.purge("/publ/sfs/1999:175/data")
         * varnishInvalidator.purge("/publ/sfs/1999:175/konsolidering/2011-05-02/data")
         *
         * To remove many objects matching a regex, use ban:
         * varnishInvalidator.ban("/publ/sfs/1999:175/")  // includes both purge examples above
         * varnishInvalidator.ban("/publ/sfs/")           // all SFS data
         * varnishInvalidator.ban("/konsolidering")       // all consolidated data
         * varnishInvalidator.ban("/2011-05-02")          // all data matching this date
         */

    }
}
