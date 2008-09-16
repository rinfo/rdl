import org.apache.commons.configuration.PropertiesConfiguration
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.collector.CollectorRunner

if (args.size() != 2) {
    println "Usage: <path-to-main-config> <uri-to-subscription-feed>"
    System.exit 0
}
def runner = new CollectorRunner(null, null)
runner.configure(new PropertiesConfiguration(args[0]))
runner.collectFeed(new URL(args[1]))

