
/* TODO:

Goals:
 - based on collector code in rinfo-main
 - services:
   - check feed-uri: run (partial?) collect (one page: follow no archive?)
   - rdf-uri: run handlers.. or feed-uri + "just entry X"?
   - feed-*source*: attrs per entry (not in collector? add-hoc "validate"?)

Needs: we need collected(depot) + collect&error-log?

 - make CollectorLog an interface? At least add rich objects to log!
   - this checker log needs both collected+collect+errors
   - "normal" skips collected (stored in main feed)

 - log method:
    - keep track of last collect in one feed
      .. fh:complete? (lossy, no tombstones etc)
      .. or pop off archives of at least failed?
      - one entry for each collected page:
          - first entry also RDF for "triggered collect"?
          - feed gets RDF uri by (feedsource?+)url+updated

View data:
 - checkLog.pages*.items*.errors -> html
   .. logObjs+stringtemplate or repo+grit+xslt

Change in rinfo-main,depot,collector:

- FeedCollector#hasVisitedArchivePage: only current: true for anything else
 - use StorageSession (subclass?):
  - dummyDepot(Backend)
    - in-mem *Backend*? (if so, depotEntry.getMetaFile -> getMetaInputStream/-Out..?)
    - or no depot? write to "/dev/null" to make datachecks? pdf:s? check some?
  - override StorageSession#storeEntry: don't break on error: always return true
  - def checkLog = new CheckDataCollectorLog

*/
@Grab('se.lagrummet.rinfo:rinfo-main:1.0-SNAPSHOT')

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
import org.restlet.resource.InputRepresentation
import org.restlet.resource.Representation
import org.restlet.resource.StringRepresentation

import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.main.storage.FeedCollector
import se.lagrummet.rinfo.main.storage.StorageSession
import se.lagrummet.rinfo.main.storage.StorageCredentials
import se.lagrummet.rinfo.main.storage.StorageHandler
import se.lagrummet.rinfo.main.storage.CollectorLog


class FeedChecker {

    def repo = new SailRepository(new MemoryStore())
    Depot depot
    File tempDir

    FeedChecker() {
        tempDir = createTempDir()
        depot = new FileDepot(new URI("http://rinfo.lagrummet.se"), tempDir)
        depot.atomizer.feedPath = "/feed"
        depot.initialize()
    }

    def checkFeed(String feedUrl) { checkFeed(new URL(feedUrl)) }

    def checkFeed(URL feedUrl) {
        def handlers = []
        def coLog = new CollectorLog(repo)
        def session = new LaxStorageSession(new StorageCredentials(false),
                depot, handlers, coLog)
        def collector = new OneFeedCollector(session)
        collector.readFeed(feedUrl)
        collector.shutdown()
        return repo
    }

    void shutdown() {
        repo.shutDown()
        removeTempDir()
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
    LaxStorageSession(StorageCredentials credentials,
            Depot depot,
            Collection<StorageHandler> storageHandlers,
            CollectorLog collectorLog) {
        super(credentials, depot, storageHandlers, collectorLog)
    }
    boolean storeEntry(Feed sourceFeed, Entry sourceEntry,
            List<SourceContent> contents, List<SourceContent> enclosures) {
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

class RInfoChecker extends Resource {

    RInfoChecker(Context context, Request request, Response response) {
        super(context, request, response)
        variants.add(new Variant(MediaType.TEXT_HTML))
    }

    @Override
    Representation represent(Variant variant) throws ResourceException {
        return new StringRepresentation("""
        <form method="POST">
            <fieldset>
                <legend>
                    <label for="feedUrl">K&auml;lla (Atom-fl&ouml;de):</label>
                </legend>
                <input type="text" class="url" id="feedUrl" />
            </fieldset>
        </form>
        """, MediaType.TEXT_HTML)
    }

    @Override boolean allowPost() { true }
    @Override void handlePost() {
        String feedUrl = request.getEntityAsForm().getFirstValue("feedUrl")
        def feedChecker = new FeedChecker()
        def logRepo = feedChecker.checkFeed(feedUrl)
        def ins = RDFUtil.toInputStream(repo, "application/rdf+xml", true)
        response.setEntity(new InputRepresentation(ins), MediaType.TEXT_XML)
        feedChecker.shutdown()
        //response.setEntity("""
        //<p>K&auml;lla: <code class="url">${feedUrl}</code>
        //</p>
        //<div id="results">
        //    <div class="feed">
        //    </div>
        //    <div class="entry">
        //    </div>
        //</div>
        //""", MediaType.TEXT_HTML)
    }

}


def serve(port) {
    def cmp = new Component()
    cmp.servers.add(Protocol.HTTP, port)
    cmp.clients.add(Protocol.FILE)
    cmp.defaultHost.attach("", new Application() {
        Restlet createRoot() {
            def router = new Router(context)
            router.attachDefault(RInfoChecker.class)
        }
    })
    cmp.start()
}

def cli = new CliBuilder()
cli.p longOpt:'port', args:1, type:int, 'port'
cli.o longOpt:'outfile', args:1, type:String, 'out file'
def opt = cli.parse(args)
if (opt.port) {
    def port = Integer.parseInt(opt.port)
    serve(port)
} else {
    opt.arguments().each {
        def feedChecker = new FeedChecker()
        try {
            def checkLogRepo = feedChecker.checkFeed(it)
            def mtype = "application/x-turtle"
            def out = opt.outfile? new FileOutputStream(new File(opt.outfile)) : System.out
            RDFUtil.serialize(checkLogRepo, mtype, out)
        } finally {
            feedChecker.shutdown()
        }
    }
}
