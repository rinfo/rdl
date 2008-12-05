import org.apache.commons.io.FileUtils
import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Application
import org.restlet.Client
import org.restlet.Component
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.data.Request

import se.lagrummet.rinfo.store.depot.*
import se.lagrummet.rinfo.store.supply.DepotFinder

import se.lagrummet.rinfo.main.FeedCollectScheduler
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


def doPing(reciever, feedUrl) {
    def request = new Request(Method.POST, reciever)
    def feedUrlMsg = "feed=${feedUrl}"
    request.setEntity(feedUrlMsg, MediaType.MULTIPART_FORM_DATA)
    //request.setReferrerRef(...)
    def client = new Client(Protocol.HTTP)
    def response = client.handle(request)
    println response.status
    println response.entity.text
}


// Initialize source depot
teststep "Create a temporary test repot"
sourceDepot = new FileDepot(
        new URI("http://example.org"), createTempDir("source"), "/feed")
// TODO: to fix bug(?) in Atomizer (doesn't set props to defaults unless configured..)
sourceDepot.atomizer.configure(new PropertiesConfiguration())
sourcePort = 8982
teststep "Start test supply"
startAppServer(sourcePort, {new TestApplication(it, sourceDepot)})
// TODO: run two separate source depots; sit back and watch what happens..


// Initialize rinfo main
teststep "Set up rinfo main"
rinfoCfg = new PropertiesConfiguration(
        "src/environments/dev-unix/rinfo-main.properties")
def tempRInfoDir = createTempDir("rinfo").toString()
rinfoCfg.setProperty("rinfo.depot.fileDir", tempRInfoDir+"/depot")
rinfoCfg.setProperty("rinfo.main.collector.registryRepoDataDir", tempRInfoDir+"/registry")
rinfoCfg.setProperty("rinfo.main.collector.sourceFeedUrls",
        [ localhost(sourcePort, "/feed/current").toString() ])
rinfoPort = 8980
teststep "Start rinfo app"
startAppServer(rinfoPort, {new MainApplication(it, rinfoCfg)})

// simulate ping from a source
def pingFeedToRInfo(feedUrl) {
    doPing("http://localhost:${rinfoPort}/collector", feedUrl)
}

// simulate ping to rinfo service
// TODO: service is started outside of this package
//servicePort = 8981
//def pingRInfoFeedToService(feedUrl) {
//    doPing("http://localhost:${servicePort}/collector", feedUrl)
//}
// TODO: Do ping from MainApplication..
//  pingRInfoFeedToService(localhost(rinfoPort, "/feed/current"))


// Test data:
DOC_1_ID = new URI("http://example.org/docs/sfs/1:1")
DOC_2_ID = new URI("http://example.org/docs/sfs/1:2")
def exampleSourceContent(fname, mtype, lang=null) {
    def base = "src/test/resources/depotdata"
    return new SourceContent(new File("$base/$fname"), mtype, lang)
}


// Case: Add
prompt("-p", "to add")
teststep "Add source entry </docs/one>"
sourceDepot.makeEntryBatch().with { batch ->
    batch << sourceDepot.createEntry(
            DOC_1_ID, new Date(), [
            exampleSourceContent("content-1.rdf", "application/rdf+xml")
        ])
    Thread.sleep(1000)
    batch << sourceDepot.createEntry(
            DOC_2_ID, new Date(), [
            exampleSourceContent("content-2.rdf", "application/rdf+xml")
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
    def entry = sourceDepot.getEntry(DOC_1_ID)
    entry.update(new Date(), [
        exampleSourceContent("content-en.pdf", "application/pdf", "en"),
        exampleSourceContent("content-1.rdf", "application/rdf+xml")
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


// Case: Read unmodified
Thread.sleep(2000)
prompt("-p", "to ping rinfo (no source mods)")
teststep "Ping rinfo-main (after no source modifications)"
pingFeedToRInfo(localhost(sourcePort, "/feed/current"))


// Case: Delete
Thread.sleep(2000) // can't modify in same second
prompt("-p", "to delete")
teststep "Delete source entry </docs/one>"
sourceDepot.makeEntryBatch().with {
    def entry = sourceDepot.getEntry(DOC_1_ID)
    entry.delete(new Date())
    it << entry
    sourceDepot.indexEntries(it)
}
prompt("-p", "to ping rinfo")
teststep "Ping rinfo-main"
pingFeedToRInfo(localhost(sourcePort, "/feed/current"))
teststep "Find deleted <publ/sfs/1:1> in rinfo:</feed/latest>"
// TODO: check resulting rinfo feed


// Teardown
Thread.sleep(2000)
prompt("-h", "to quit")
tearDown()

