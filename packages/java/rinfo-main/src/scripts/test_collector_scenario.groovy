import org.apache.commons.io.FileUtils
import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Application
import org.restlet.Component
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.Protocol

import se.lagrummet.rinfo.store.depot.*
import se.lagrummet.rinfo.store.supply.DepotFinder

import se.lagrummet.rinfo.collector.CollectorRunner
import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.main.MainApplication


// Test env setup

// TODO: config logging <http://java.sun.com/j2se/1.5.0/docs/api/index.html?java/util/logging/LogManager.html>
//System.setProperty("java.util.logging.config.file",
//        "/your/path/logging.config");

tempDirs = []
def createTempDir(name) {
    tempDir = File.createTempFile("rinfo-${name}-", "",
            new File(System.getProperty("java.io.tmpdir")))
    assert tempDir.delete(); assert tempDir.mkdir()
    tempDirs << tempDir
    return tempDir
}

appServers = []
def startAppServer(port, makeApp) {
    def component = new Component()
    component.servers.add(Protocol.HTTP, port)
    component.defaultHost.attach(
            makeApp(component.context.createChildContext()) )
    component.start()
    appServers << component
}

def tearDown() {
    appServers.each { it.stop() }
    tempDirs.each { FileUtils.forceDelete(it) }
}

// Utils

class TestApplication extends Application {
    def fileDepot
    TestApplication(parentContext, fileDepot) {
        super(parentContext); this.fileDepot = fileDepot
    }
    synchronized Restlet createRoot() {
        return new DepotFinder(context, fileDepot)
    }
}

def localhost(port, path) { new URL("http://localhost:${port}${path}") }

def prompt(flag, message=null) {
    message = message ?: ""
    if (flag in args) {
        println()
        while (true) {
            println(); print ">>> Type ENTER ${message}.."
            if (System.in.read() == "\n") break;
        }
        println()
    }
}

def teststep(msg) {
    println "="*20; println msg; println "="*20
}

// Initialize source depot
teststep "Create a temporary test repot"
sourceDepot = new FileDepot(
        new URI("http://example.org"), createTempDir("source"), "/feed")
sourcePort = 8182
teststep "Start test supply"
startAppServer(sourcePort, {new TestApplication(it, sourceDepot)})
// TODO: run two separate source depots; sit back and watch what happens..


// Initialize rinfo main
teststep "Set up rinfo main"
rinfoCfg = new PropertiesConfiguration(
        "src/environments/dev-unix/rinfo-main.properties")
rinfoCfg.setProperty("rinfo.depot.fileDir", createTempDir("rinfo").toString())
rinfoPort = 8180
teststep "Start rinfo app"
startAppServer(rinfoPort, {new MainApplication(it, rinfoCfg)})

// simulate ping from a source
def pingFeedToRInfo(feedUrl) {
    def pingUrl = new URL(
            "http://localhost:${rinfoPort}/collector?feed=${feedUrl}")
    println pingUrl.text
}


// Initialize rinfo service
teststep "Set up rinfo service"
import se.lagrummet.rinfo.service.RDFStoreLoaderRestlet
class ServiceApplication extends Application {
    ServiceApplication(parentContext) {
        super(parentContext)
    }
    synchronized Restlet createRoot() {
        return new RDFStoreLoaderRestlet(context,
                "http://localhost:8080/openrdf-sesame", "rinfo")
    }
}
servicePort = 8181
startAppServer(servicePort, {new ServiceApplication(it)})
// TODO: do ping from MainApplication..
def pingRInfoFeedToService(feedUrl) {
    def pingUrl = new URL(
            "http://localhost:${servicePort}/?feed=${feedUrl}")
    println pingUrl.text
}


// Test data:
DOC_ONE_ID = new URI("http://example.org/docs/sfs/1:1")
def exampleSourceContent(fname, mtype, lang=null) {
    def base = "src/test/resources/depotdata"
    return new SourceContent(new File("$base/$fname"), mtype, lang)
}


// Case: Add
prompt("-p", "to add")
teststep "Add source entry </docs/one>"
sourceDepot.makeEntryBatch().with { batch ->
    batch << sourceDepot.createEntry(
            DOC_ONE_ID, new Date(), [
            exampleSourceContent("content.rdf", "application/rdf+xml")
        ])
    sourceDepot.indexEntries(batch)
}
prompt("-p", "to ping rinfo")
teststep "Ping rinfo-main"
pingFeedToRInfo(localhost(sourcePort, "/feed/current"))
teststep "Find created==updated <publ/sfs/1:1> in rinfo:</feed/latest>"
// TODO: check resulting rinfo feed


// Case: Update
Thread.sleep(2000) // can't modify in same second
prompt("-p", "to update")
teststep "Update source entry </docs/one>"
sourceDepot.makeEntryBatch().with {
    def entry = sourceDepot.getEntry(DOC_ONE_ID)
    entry.update(new Date(), [
        exampleSourceContent("content-en.pdf", "application/pdf", "en"),
        exampleSourceContent("content.rdf", "application/rdf+xml")
    ])
    it << entry
    sourceDepot.indexEntries(it)
}
prompt("-p", "to ping rinfo")
teststep "Ping rinfo-main"
pingFeedToRInfo(localhost(sourcePort, "/feed/current"))
// FIXME: adds an updated twice in index! (see FeedCollector)
teststep "Find updated <publ/sfs/1:1> in rinfo:</feed/current>"
// TODO: check resulting rinfo feed


// Case: Delete
/* FIXME: collector can't yet handle these!
Thread.sleep(2000) // can't modify in same second
prompt("-p", "to delete")
teststep "Delete source entry </docs/one>"
sourceDepot.makeEntryBatch().with {
    def entry = sourceDepot.getEntry(DOC_ONE_ID)
    entry.delete(new Date())
    it << entry
    sourceDepot.indexEntries(it)
}
prompt("-p", "to ping rinfo")
teststep "Ping rinfo-main"
pingFeedToRInfo(localhost(sourcePort, "/feed/current"))
teststep "Find deleted <publ/sfs/1:1> in rinfo:</feed/latest>"
// TODO: check resulting rinfo feed
*/


// Case: Read unmodified
Thread.sleep(1000)
prompt("-p", "to ping rinfo (no source mods)")
teststep "Ping rinfo-main (after no source modifications)"
pingFeedToRInfo(localhost(sourcePort, "/feed/current"))


// TODO: this is a manual ping, do from main..
Thread.sleep(1000)
prompt("-p", "to ping service")
teststep "Pinging rinfo-service (collect to triple store)"
pingRInfoFeedToService(localhost(rinfoPort, "/feed/current"))


// TODO: Add to each step: SPAQRL-query for $entryUri.


// Teardown
prompt("-h", "to quit")
tearDown()
//System.exit 0

