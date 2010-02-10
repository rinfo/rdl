
// TODO:
// - rinfo-main: make CollectorLog an interface
// - dummyDepot with store noop (should write to "/dev/null" to make datachecks)
// - use StorageSession:
//  - dummyDepot(Backend)
//  - def checkLog = new CheckDataCollectorLog
//  - override StorageSession#storeEntry - always return true
//  - checkLog.pages*.items*.errors -> stringtemplate->html
@Grab('se.lagrummet.rinfo:rinfo-main:1.0-SNAPSHOT')
import org.restlet.*
import org.restlet.data.Protocol
import org.restlet.data.*
import org.restlet.resource.Resource
import org.restlet.resource.Variant
import org.restlet.resource.Representation
import org.restlet.resource.StringRepresentation

import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.main.storage.FeedCollector
import se.lagrummet.rinfo.main.storage.StorageSession
import se.lagrummet.rinfo.main.storage.StorageCredentials
import se.lagrummet.rinfo.main.storage.CollectorLog


class FeedChecker {
    def checkFeed(feedUrl) {
        def depot = new Depot() {}
        def handlers = []
        def repo = new SailRepository(new MemoryStore())
        def coLog = new CollectorLog(repo)
        def session = new StorageSession(new StorageCredentials(false),
                depot, handlers, coLog)
        FeedCollector.readFeed(session, feedUrl)
        //def mtype = "application/x-turtle"
        //RDFUtil.serialize(repo, mtype, System.out)
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
            <input type="text" class="url" name="feedUrl" />
        </form>
        """, MediaType.TEXT_HTML)
    }

    @Override boolean allowPost() { true }
    @Override void handlePost() {
        String feedUrl = request.getEntityAsForm().getFirstValue("feedUrl")
        new FeedChecker().checkFeed(feedUrl)
        response.setEntity("""
        <p>Processed <code class="url">${feedUrl}</code>
        </p>
        """, MediaType.TEXT_HTML)
    }

}


def run(port) {
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

def port = args.length > 0? args[0] : 8280
run(port)

