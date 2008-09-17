package se.lagrummet.rinfo.service

import org.restlet.Application
import org.restlet.Context
import org.restlet.Finder
import org.restlet.Handler
import org.restlet.Restlet
import org.restlet.Router
import org.restlet.data.CharacterSet
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration

import org.openrdf.repository.Repository
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore


class ServiceApplication extends Application {

    static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service.properties"
    static final String RDF_LOADER_CONTEXT_KEY = "rinfo.service.rdfloader"

    Repository repo
    SesameLoader rdfStoreLoader

    ServiceApplication(Context parentContext) {
        super(parentContext)
        configure(new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME))
    }

    void configure(AbstractConfiguration config) {
        def repoPath = config.getString("rinfo.service.sesameRepoPath")
        def remoteRepoName = config.getString("rinfo.service.sesameRemoteRepoName")
        if (repoPath =~ /^https?:/) {
            repo = new HTTPRepository(repoPath, remoteRepoName)
        } else {
            def dataDir = new File(repoPath)
            repo = new SailRepository(new NativeStore(dataDir))
        }
        repo.initialize()
        rdfStoreLoader = new SesameLoader(repo)
        attrs.putIfAbsent(RDF_LOADER_CONTEXT_KEY, rdfStoreLoader)
    }

    @Override
    synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attachDefault(new Finder(getContext(), RDFStoreLoader))
        return router
    }

    @Override
    public void stop() {
        super.stop()
        repo.shutdown()
    }

}


class RDFStoreLoader extends Handler {

    @Override
    public boolean allowPost() { return true; }

    @Override
    public void handlePost() {
        String feedUrl = request.getResourceRef().
                getQueryAsForm(CharacterSet.UTF_8).getFirstValue("feed")
        if (feedUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing feed parameter.")
            return
        }

        def rdfStoreLoader = (RDFStoreLoader) getContext().getAttributes().get(
                ServiceApplication.RDF_LOADER_CONTEXT_KEY)

        // FIXME: run via concurrent!
        // FIXME: loader needs "stop at entryId + date" or something..
        loadFromFeed(new URL(feedUrl))
        rdfStoreLoader.readFeed(feedUrl)
        response.setEntity("Scheduled collect of <${feedUrl}>.", MediaType.TEXT_PLAIN)
    }

}
