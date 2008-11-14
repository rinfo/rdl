package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory
import org.slf4j.Logger

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
import org.restlet.data.Status
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration

import org.openrdf.repository.Repository
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper
import org.openrdf.sail.nativerdf.NativeStore

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ServiceApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service.properties"
    public static final String RDF_REPO_CONTEXT_KEY = "rinfo.service.rdfrepo"
    public static final String THREAD_POOL_CONTEXT_KEY = "rinfo.service.threadpool"
    public static final String LOGGER_CONTEXT_KEY = "rinfo.service.logger"

    private final logger = LoggerFactory.getLogger(ServiceApplication)

    private ExecutorService threadPool    
    private Repository repo
    
    public ServiceApplication(Context parentContext) {
        super(parentContext)        
        
        threadPool = Executors.newSingleThreadExecutor()

        def attrs = getContext().getAttributes()
        attrs.putIfAbsent(THREAD_POOL_CONTEXT_KEY, threadPool)
        attrs.putIfAbsent(LOGGER_CONTEXT_KEY, logger)        

        configure(new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME))
    }

    protected void configure(AbstractConfiguration config) {
    	def repoPath = config.getString("rinfo.service.sesameRepoPath")
        def remoteRepoName = config.getString("rinfo.service.sesameRemoteRepoName")
        
        if (repo != null) {
            // close previous repo if set - to enable reconfiguration
            repo.shutDown()
        }

        if (repoPath =~ /^https?:/) {
            repo = new HTTPRepository(repoPath, remoteRepoName)
        } else {
            def dataDir = new File(repoPath)
            repo = new SailRepository(new NativeStore(dataDir))
        }
        repo = new NotifyingRepositoryWrapper(repo) // enable notifications     
        repo.initialize()
        
        def attrs = getContext().getAttributes()
        attrs.put(RDF_REPO_CONTEXT_KEY, repo)
    }

    protected void addRepositoryListener(listener) {
    	repo.addRepositoryListener(listener)    	
    }

    protected void addRepositoryConnectionListener(listener) {
    	repo.addRepositoryConnectionListener(listener)     
    }

    @Override
    public synchronized Restlet createRoot() {
        def router = new Router(getContext())                
        router.attach("/status", StatusResource)        
        router.attachDefault(new Finder(getContext(), RDFLoaderHandler))
        return router
    }

    @Override
    public void stop() {
        super.stop()
        threadPool.shutdown()
        repo.shutDown()
    }

}


class RDFLoaderHandler extends Handler {

    @Override
    public boolean allowPost() { return true; }

    @Override
    public void handlePost() {
        String feedUrl = request.getEntityAsForm().getFirstValue("feed")
        if (feedUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing feed parameter.")
            return
        }
        logger.info("ServiceApplication: Scheduling collect of <${feedUrl}>.")
        triggerFeedCollect(new URL(feedUrl))
        response.setEntity("Scheduled collect of <${feedUrl}>.", MediaType.TEXT_PLAIN)
    }

    boolean triggerFeedCollect(URL feedUrl) {
        // TODO: verify source of request and/or feedUrl
        // FIXME: error handling.. (report and/or (public) log)

        def repo = (Repository) getContext().getAttributes().get(
                ServiceApplication.RDF_REPO_CONTEXT_KEY)
                
        def pool = (ExecutorService) getContext().getAttributes().get(
                ServiceApplication.THREAD_POOL_CONTEXT_KEY) 

        def logger = (Logger) getContext().getAttributes().get(
        		ServiceApplication.LOGGER_CONTEXT_KEY) 
        		
		pool.execute({
            logger.info("Beginning collect of <${feedUrl}>.")            
            // TODO:IMPROVE: Ok to make a new instance for each request? Shouldn't
            // be so expensive, and isolates it (shouldn't be app-global..)
            def rdfStoreLoader = new SesameLoader(repo)
            rdfStoreLoader.readFeed(feedUrl)
            logger.info("Completed collect of <${feedUrl}>.")                
        })
        return true
    }

}

/*
 *  Basic resource for simple status message.   
 *  
 *  TODO: replace this by a handleGet in RDFLoaderHandler?
 *  TODO: some form of collect status page..?
 */
class StatusResource extends Resource {
	public StatusResource(Context context, Request request, Response response) {
		super(context, request, response)
		getVariants().add(new Variant(MediaType.TEXT_PLAIN))
	}
	@Override
	public Representation getRepresentation(Variant variant) {
		def representation = new StringRepresentation("OK", MediaType.TEXT_PLAIN)
		return representation
	}
}
