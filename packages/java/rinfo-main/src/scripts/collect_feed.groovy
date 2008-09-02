import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.collector.FeedCollector
import se.lagrummet.rinfo.base.URIMinter


if (args.size() != 1) {
    println "Usage: <uri-to-subscription-feed>"
    System.exit 0
}
def depot = FileDepot.autoConfigure()

def rinfoBaseDir = "../../../resources/base/"
def uriMinter = new URIMinter(rinfoBaseDir)

def collector = new FeedCollector(depot, uriMinter)
collector.readFeed(new URL(args[0]))

depot.generateIndex()

