import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.collector.FeedCollector
import se.lagrummet.rinfo.base.URIMinter


if (args.size() != 2) {
    println "Usage: <path-to-depot-props> <uri-to-subscription-feed>"
    System.exit 0
}
def depot = FileDepot.autoConfigure(args[0])

def rinfoBaseDir = "../../../resources/base/"
def uriMinter = new URIMinter(rinfoBaseDir)

def collector = new FeedCollector(depot, uriMinter)
collector.readFeed(new URL(args[1]))
//depot.generateIndex()

