@Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
@Grab('org.restlet.jse:org.restlet:2.0.1')
@Grab('org.restlet.jee:org.restlet.ext.servlet:2.0.1')
@Grab('org.slf4j:slf4j-simple:1.6.1')
import se.lagrummet.rinfo.store.depot.*

def commands = [

    find: { depot, feed, findIds ->
        println "Scanning feed <${feed.selfLink.href}>..."
        def found = feed.entries.find { it.id.toString() in findIds }
        if (found) {
            println "Found <${found.id}>!"
            return false
        }
    },

    check: { depot, feed, _ ->
        def missing = []
        feed.entries.each {
            def uri = it.id.toURI()
            if (!depot.getEntry(uri)) {
                missing << uri
            }
        }
        if (missing) {
            println "Missing entries in <${feed.selfLink.href}>:"
            missing.each { println "<${it}>" }
            println()
        }
    },

    //rmlocks: ...
    //index: ...

]

if (args.length < 2) {
    println "Usage: depot_tool <base-dir> <${commands.keySet().join('|')}> [...]"
    System.exit 0
}
def baseDir = args[0] as File
def baseUri = new URI("http://rinfo.lagrummet.se")
def command = commands[args[1]]
assert command
def subargs = args.length > 2? args[2..-1] : []

def depot = new FileDepot(baseDir: baseDir, baseUri: baseUri)
depot.atomizer.feedPath = "/feed"
depot.initialize()

def indexer = new AtomIndexer(depot.atomizer, depot.backend)

def startFeed = indexer.getFeed(depot.atomizer.subscriptionPath)
def feed = startFeed

while (feed) {
    if (command(depot, feed, subargs) == false) {
        break
    }
    feed = indexer.getPrevArchiveAsFeed(feed)
}

