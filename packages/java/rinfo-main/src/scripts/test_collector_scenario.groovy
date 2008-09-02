import org.apache.commons.io.FileUtils

import org.restlet.Application
import org.restlet.Component
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.Protocol

import se.lagrummet.rinfo.store.depot.*
import se.lagrummet.rinfo.store.supply.DepotFinder

import se.lagrummet.rinfo.collector.FeedCollector
import se.lagrummet.rinfo.base.URIMinter


// Utils (and "test state")
tempDirs = []
def createTempDir(name) {
    tempDir = File.createTempFile("rinfo-${name}-", "",
            new File(System.getProperty("java.io.tmpdir")))
    assert tempDir.delete(); assert tempDir.mkdir()
    tempDirs << tempDir
    return tempDir
}

class TestApplication extends Application {
    def fileDepot
    TestApplication(parentContext, fileDepot) {
        super(parentContext); this.fileDepot = fileDepot
    }
    synchronized Restlet createRoot() {
        return new DepotFinder(context, fileDepot)
    }
}
def startAppServer(port, makeApp) {
    // TODO: suppress/reduce logging? And/or wait for server ready?
    return new Component().with {
        servers.add(Protocol.HTTP, port)
        def app = makeApp(context.createChildContext())
        defaultHost.attach(app)
        start()
    }
}

def localhost(port, path) { new URL("http://localhost:${port}${path}") }

def tearDown() {
    tempDirs.each { FileUtils.forceDelete(it) }
}


// Initialize source depot
println "Create a temporary test repot"
sourceDepot = new FileDepot(
        new URI("http://example.org"), createTempDir("source"), "/feed")

sourcePort = 8182
println "Start test supply"
startAppServer(sourcePort, {new TestApplication(it, sourceDepot)})


// Initialize rinfo app
// FIXME: turn assemblage to RInfoApplication (replacing TestApplication)..
println "Assemble rinfo parts"
rinfoDepot = new FileDepot(
        new URI("http://rinfo.lagrummet.se"), createTempDir("rinfo"), "/feed")
RINFO_BASE_DIR = "../../../resources/base/"
uriMinter = new URIMinter(RINFO_BASE_DIR)

rinfoPort = 8180
println "Start rinfo app"
startAppServer(rinfoPort, {new TestApplication(it, rinfoDepot)})


// Test data:
DOC_ONE_ID = new URI("http://example.org/docs/sfs/1:1")
def sourceContentRdf(_filePath) {
    filePath = "src/test/resources/depotdata/content.rdf"
    return new SourceContent(new File(filePath), "application/rdf+xml")
}


// Case: Add
println "Add entry <docs/one>"
sourceDepot.makeEntryBatch().with { batch ->
    batch << sourceDepot.createEntry(
            DOC_ONE_ID, new Date(), [sourceContentRdf("content.rdf")])
    sourceDepot.indexEntries(batch)
}
println "Ping rinfo-main"
// TODO: ping ("from source")!
new FeedCollector(rinfoDepot, uriMinter).readFeed(
        localhost(sourcePort, "/feed/current"))
println "Find created==updated <publ/sfs/1:1> in rinfo:</feed/latest>"
// TODO


// Case: Update
println "Update entry </docs/one>"
sourceDepot.makeEntryBatch().with {
    entry = sourceDepot.getEntry(DOC_ONE_ID)
    entry.update(new Date(), [sourceContentRdf("content.rdf")])
    it << entry
    sourceDepot.indexEntries(it)
}
Thread.sleep(1000) // can't modify in same second
println "Ping rinfo-main"
// TODO: ping ("from source")!
collector = new FeedCollector(rinfoDepot, uriMinter)
collector.readFeed(
        localhost(sourcePort, "/feed/current"))
// FIXME: adds an updated twice in index! (it must stop at last datetime!)
// .. this is also bad since collector *first* tries to create - and succeeds
//    which is why the entry seems first updated, then "duplicated"
println "DEBUG: collectedBatch: "+collector.collectedBatch.size()
println "Find updated <publ/sfs/1:1> in rinfo:</feed/latest>"
// TODO


// Case: Delete
/* TODO: activate
Thread.sleep(1000) // can't modify in same second
println "Delete entry /docs/<one>"
sourceDepot.getEntry(DOC_ONE_ID).delete(new Date())
println "Ping rinfo-main"
println "Find deleted <publ/sfs/1:1> in rinfo:</feed/latest>"
*/


// Teardown
if ("-h" in args) {
    while (true) {
        println(); print "Type ENTER to quit..."
        if (System.in.read() == "\n") break;
    }
}
tearDown()
System.exit 0


// TODO: Add to each step: SPAQRL-query for $entryUri.
