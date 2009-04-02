import org.apache.commons.configuration.PropertiesConfiguration
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.main.FeedCollectScheduler

if (args.size() != 2) {
    println "Usage: <path-to-main-config> <uri-to-subscription-feed>"
    System.exit 0
}
def feedCollectScheduler = new FeedCollectScheduler(null, null)
feedCollectScheduler.configure(new PropertiesConfiguration(args[0]))
feedCollectScheduler.triggerFeedCollect(new URL(args[1]))

