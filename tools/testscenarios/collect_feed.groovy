import se.lagrummet.rinfo.store.depot.FileDepot
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.main.storage.FeedCollector
import se.lagrummet.rinfo.main.storage.StorageSession
import se.lagrummet.rinfo.main.storage.StorageCredentials
import se.lagrummet.rinfo.main.storage.CollectorLog


@Grab(group='se.lagrummet.rinfo', module='rinfo-main', version='1.0-SNAPSHOT')
def collectFeed(baseDir, feedUrl) {
    def depot = createDepot(baseDir)
    def handlers = []
    def repo = new SailRepository(new MemoryStore())
    def coLog = new CollectorLog(repo)
    def session = new StorageSession(new StorageCredentials(false),
            depot, handlers, coLog)
    FeedCollector.readFeed(session, feedUrl)
    //def mtype = "application/x-turtle"
    //RDFUtil.serialize(repo, mtype, System.out)
}

def createDepot(baseDir) {
    def depot = new FileDepot()
    depot.baseDir = baseDir
    depot.baseUri = new URI("http://rinfo.lagrummet.se")
    depot.atomizer.feedPath = "/feed"
    depot.atomizer.feedSkeletonPath = "feed_skeleton.atom"
    return depot
}

def reindexDepot(baseDir) {
    def depot = createDepot(baseDir)
    def session = depot.openSession()
    session.generateIndex()
    session.close()
}


if (args.size() < 1 || args.size() > 2) {
    println "Usage: <path-to-depot-dir> <uri-to-subscription-feed>"
    System.exit 0
} else if (args.size() == 2) {
    collectFeed(new File(args[0]), new URL(args[1]))
} else {
    reindexDepot(new File(args[0]))
}

