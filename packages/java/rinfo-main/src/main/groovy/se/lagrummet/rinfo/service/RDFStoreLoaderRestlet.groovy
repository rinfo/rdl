package se.lagrummet.rinfo.service

import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.CharacterSet
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response

import org.openrdf.repository.Repository
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore


class RDFStoreLoaderRestlet extends Restlet {

    static final ALLOWED = new HashSet([Method.GET])
    private FileDepot depot
    String repoPath
    String remoteRepoName

    public RDFStoreLoaderRestlet(Context context, FileDepot depot, repoPath, remoteRepoName) {
        super(context)
        this.depot = depot
        this.repoPath = repoPath
        this.remoteRepoName = remoteRepoName
    }

    @Override
    public void handle(Request request, Response response) {
        String feedUrl = request.getResourceRef().
                getQueryAsForm(CharacterSet.UTF_8).getFirstValue("feed")
        response.setAllowedMethods(ALLOWED)
        if (feedUrl == null) {
            response.setEntity("No feed parameter.", MediaType.TEXT_PLAIN)
            return
        }
        // FIXME: run via concurrent!
        loadFromFeed(new URL(feedUrl))
        response.setEntity("Scheduled collect of <${feedUrl}>.", MediaType.TEXT_PLAIN)
    }

    private void loadFromFeed(URL feedUrl) {
        Repository repo = null
        def repoPath = args[1]
        if (repoPath =~ /^https?:/) {
            repo = new HTTPRepository(repoPath, repoName)
        } else {
            def dataDir = new File(repoPath)
            repo = new SailRepository(new NativeStore(dataDir))
        }
        repo.initialize()
        // FIXME: loader needs "stop at entryId + date" or something..
        SesameLoader rdfStoreLoader = new SesameLoader(repo)
        rdfStoreLoader.readFeed([new URL(feedUrl)])
        repo.shutDown()
    }

}
