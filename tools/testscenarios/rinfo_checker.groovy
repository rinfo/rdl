
/* TODO:

Goals:
 - based on collector code in rinfo-main
 - services:
   - check feed by url: run (partial?) collect (one page: follow no archive?)
   - check rdf by url: run handlers.. or feed-uri + "just entry X"?
   - check feed *source* (body)): attrs per entry (not in collector? add-hoc "validate"?)

Suggested changes in rinfo-main,-depot,-collector:

 - make CollectorLog an interface? At least configurable..
   - this checker log needs both collected+collect+errors
   - "normal" skips collected (stored in main feed)
     .. in that mode, we'd need to combine depot feed (as collect success log) + checkLog

 - StorageSession (subclass?):
   - dummyDepot(Backend)
     - in-mem *Backend*? (if so, depotEntry.getMetaFile -> getMetaInputStream/-Out..?)
     - or no depot? write to "/dev/null" to make datachecks? pdf:s? check some?
  - def checkLog = new CheckDataCollectorLog

*/
@Grab('se.lagrummet.rinfo:rinfo-main:1.0-SNAPSHOT')

import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import org.xml.sax.InputSource

import org.apache.commons.io.FileUtils

import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore

import org.restlet.*
import org.restlet.data.Protocol
import org.restlet.data.*
import org.restlet.resource.Resource
import org.restlet.resource.Variant
import org.restlet.resource.Representation
import org.restlet.resource.StringRepresentation

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.URIMinter

import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.main.storage.FeedCollector
import se.lagrummet.rinfo.main.storage.StorageSession
import se.lagrummet.rinfo.main.storage.StorageCredentials
import se.lagrummet.rinfo.main.storage.StorageHandler
import se.lagrummet.rinfo.main.storage.CollectorLog
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler


class Checker {

    def repo = new SailRepository(new MemoryStore())
    Depot depot
    File tempDir
    def handlers = []
    int maxEntries = -1

    Checker() {
        tempDir = createTempDir()
        depot = new FileDepot(new URI("http://rinfo.lagrummet.se"), tempDir)
        depot.atomizer.feedPath = "/feed"
        depot.initialize()
    }

    void shutdown() {
        repo.shutDown()
        removeTempDir()
    }

    def checkFeed(String feedUrl) { checkFeed(new URL(feedUrl)) }

    def checkFeed(URL feedUrl) {
        def coLog = new CollectorLog(repo)
        def storageSession = new LaxStorageSession(new StorageCredentials(false),
                depot, handlers, coLog)
        storageSession.maxEntries = maxEntries
        def collector = new OneFeedCollector(storageSession)
        collector.readFeed(feedUrl)
        collector.shutdown()
        return repo
    }

    File createTempDir() {
        int i = 0
        def tempDir
        while (true) {
            i++
            def dirName = "rinfo-feedchecker-dir-"+i
            tempDir = new File(System.getProperty("java.io.tmpdir"), dirName)
            if (!tempDir.exists())
                break
        }
        assert tempDir.mkdir()
        return tempDir
    }

    void removeTempDir() {
        FileUtils.forceDelete(tempDir)
    }

}

class LaxStorageSession extends StorageSession {
    int maxEntries = -1
    int visitedEntries = 0
    LaxStorageSession(StorageCredentials credentials,
            Depot depot,
            Collection<StorageHandler> storageHandlers,
            CollectorLog collectorLog) {
        super(credentials, depot, storageHandlers, collectorLog)
    }
    boolean storeEntry(Feed sourceFeed, Entry sourceEntry,
            List<SourceContent> contents, List<SourceContent> enclosures) {
        if (maxEntries > -1 && visitedEntries == maxEntries) {
            return false
        }
        visitedEntries++
        super.storeEntry(
                sourceFeed, sourceEntry, contents, enclosures)
        return true // never break on error
    }
}

class OneFeedCollector extends FeedCollector {
    OneFeedCollector(StorageSession storageSession) {
        super(storageSession)
    }
    int archiveCount = 0
    boolean hasVisitedArchivePage(URL pageUrl) {
        // not visited current; true for anything else
        if (archiveCount >= 1) {
            return true
        }
        archiveCount++
        return false
    }
}

class TransformerUtil {

    static saxTf = (SAXTransformerFactory) TransformerFactory.newInstance()

    static String toXhtml(inputStream, xslts) {
        def filter = null
        for (String xslt : xslts) {
            def tplt = saxTf.newTemplates(new StreamSource(xslt))
            def nextFilter = saxTf.newXMLFilter(tplt)
            if (filter) nextFilter.setParent(filter)
            filter = nextFilter
        }
        def transformSource = new SAXSource(filter, new InputSource(inputStream))
        def htmlTransformer = saxTf.newTransformer()
        [   (OutputKeys.METHOD): "xml",
            (OutputKeys.OMIT_XML_DECLARATION): "yes",
            (OutputKeys.DOCTYPE_PUBLIC): "-//W3C//DTD XHTML 1.0 Strict//EN",
            (OutputKeys.DOCTYPE_SYSTEM): "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
        ].each htmlTransformer.&setOutputProperty
        def strWriter = new StringWriter()
        htmlTransformer.transform(transformSource, new StreamResult(strWriter))
        return strWriter.toString()
    }

}


class CheckerResource extends Resource {

    CheckerResource(Context context, Request request, Response response) {
        super(context, request, response)
        variants.add(new Variant(MediaType.TEXT_HTML))
        // TODO: get config (xslts..) from context
    }

    static inputFormHtml = """
        <form method="POST">
            <fieldset>
                <legend>
                    <label for="feedUrl">K&auml;lla (Atom-fl&ouml;de):</label>
                </legend>
                <p>
                    <label for="feedUrl">URL</label>
                    <input type="text" class="url" name="feedUrl" id="feedUrl"
                        size="64" />
                </p>
                <p>
                    <label for="feedUrl">Antal poster att unders&ouml;ka</label>
                    <input type="text" name="maxEntries" id="maxEntries"
                        size="3" value="10" />
                </p>
                <p>
                    <input type="submit" value="Check" />
                </p>
            </fieldset>
        </form>
        """

    @Override
    Representation represent(Variant variant) throws ResourceException {
        return new StringRepresentation(inputFormHtml, MediaType.TEXT_HTML)
    }

    @Override boolean allowPost() { return true }

    @Override
    void handlePost() {
        def form = request.getEntityAsForm()
        String feedUrl = form.getFirstValue("feedUrl")
        String maxEntriesStr = form.getFirstValue("maxEntries")
        int maxEntries = maxEntriesStr ? Integer.parseInt(maxEntriesStr) : -1
        def handlers = context.getAttributes().get("handlers")
        def checker = new Checker(maxEntries:maxEntries, handlers:handlers)
        try {
            def logRepo = checker.checkFeed(feedUrl)
            def ins = RDFUtil.toInputStream(logRepo, "application/rdf+xml", true)
            // TODO: pass prepared templates objects instead (and e.g. mediabase param)
            def html = TransformerUtil.toXhtml(ins,
                    ["../../resources/external/xslt/rdfxml-grit.xslt",
                        "rinfo-checker-collector-log.xslt"]
                )
            response.setEntity(new StringRepresentation(html, MediaType.TEXT_HTML))
        } finally {
            checker.shutdown()
        }
    }

}


def serve(port, handlers) {
    def cmp = new Component()
    cmp.servers.add(Protocol.HTTP, port)
    cmp.clients.add(Protocol.FILE)
    cmp.defaultHost.attach("", new Application() {
        Restlet createRoot() {
            getContext().getAttributes().putIfAbsent("handlers", handlers)
            def router = new Router(getContext())
            router.attachDefault(CheckerResource.class)
            router.attach("/media", new Directory(getContext(),
                                            new File("media").toURL().toString()))
            return router
        }
    })
    cmp.start()
}


def cli = new CliBuilder()
cli.p longOpt:'port', args:1, type:int, 'port'
cli.o longOpt:'outfile', args:1, 'out file'
cli.l longOpt:'entrylimit', args:1, type:int, 'limit maximum of collected entries'
cli.m longOpt:'minterentry', args:1, 'depot entry to read uri minter config from'
def opt = cli.parse(args)

def handlers = []
def uriMinterHandler = new EntryRdfValidatorHandler(
        new URI("http://rinfo.lagrummet.se/sys/uri"), "/publ/")
if (opt.minterentry) {
    uriMinterHandler.setUriMinter(new URIMinter(RDFUtil.slurpRdf(opt.minterentry)))
    handlers << uriMinterHandler
}

if (opt.port) {
    def port = Integer.parseInt(opt.port)
    serve(port, handlers)
} else {
    opt.arguments().each {
        def maxEntries = opt.entrylimit? Integer.parseInt(opt.entrylimit) : -1
        def checker = new Checker(maxEntries:maxEntries, handlers:handlers)
        try {
            def checkLogRepo = checker.checkFeed(it)
            def conn = checkLogRepo.connection
            def ns = conn.&setNamespace
            ns("xsd", "http://www.w3.org/2001/XMLSchema#")
            ns("rx", "http://www.w3.org/2008/09/rx#")
            ns("dct", "http://purl.org/dc/terms/")
            ns("awol", "http://bblfish.net/work/atom-owl/2006-06-06/#")
            ns("iana", "http://www.iana.org/assignments/relation/")
            ns("tl", "http://purl.org/NET/c4dm/timeline.owl#")
            ns("rc", "http://rinfo.lagrummet.se/ns/2008/10/collector#")
            conn.close()
            def mtype = "application/x-turtle"
            def out = opt.outfile? new FileOutputStream(new File(opt.outfile)) : System.out
            RDFUtil.serialize(checkLogRepo, mtype, out)
        } finally {
            checker.shutdown()
        }
    }
}
