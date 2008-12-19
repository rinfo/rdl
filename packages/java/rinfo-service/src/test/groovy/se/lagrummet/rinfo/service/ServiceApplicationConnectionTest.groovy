package se.lagrummet.rinfo.service

import org.junit.*
import static org.junit.Assert.*

import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Client
import org.restlet.Component
import org.restlet.VirtualHost
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status

import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.event.RepositoryListener;
import se.lagrummet.rinfo.service.util.FeedApplication
import se.lagrummet.rinfo.service.util.RepoListener
import se.lagrummet.rinfo.service.util.RepoConnectionListener

/**
 * Test for verifying ServiceApplication's handling of multiple connections.
 * 
 */
class ServiceApplicationConnectionTest {

    static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service.properties"

    static serviceAppUrl
    static feedAppUrl
    static sesameRepoPath
    static repoListener
    static component
    
    @BeforeClass
    static void setupClass() {      
        def config = new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME)
        def serviceAppPort = config.getInt("rinfo.service.serviceAppPort")
        def feedAppPort = config.getInt("rinfo.service.feedAppPort")
        def appUrlBase = config.getString("rinfo.service.appUrlBase")
        serviceAppUrl = appUrlBase + ":" + serviceAppPort
        feedAppUrl = appUrlBase + ":" + feedAppPort
        sesameRepoPath = config.getString("rinfo.service.sesameRepoPath")        

        component = new Component()
        def context = component.getContext()

        // create a local ServiceApplication
        def serviceApplication = new ServiceApplication(context.createChildContext())

        repoListener = new RListener()
        serviceApplication.loadScheduler.addRepositoryListener(repoListener)
        serviceApplication.loadScheduler.addRepositoryConnectionListener(repoListener)
        
        // create a local application that serves feeds
        def feedApplication = new FeedApplication(context.createChildContext())     
        
        // start applications
        def feedHost = new VirtualHost(context.createChildContext())
        feedHost.setHostPort("" + feedAppPort)
        feedHost.attach(feedApplication)

        def serviceHost = new VirtualHost(context.createChildContext())
        serviceHost.setHostPort("" + serviceAppPort)
        serviceHost.attach(serviceApplication)
        
        component.with {
            servers.add(Protocol.HTTP, serviceAppPort)
            servers.add(Protocol.HTTP, feedAppPort)
            clients.add(Protocol.HTTP)
            clients.add(Protocol.FILE)            
            hosts.add(serviceHost)            
            hosts.add(feedHost)
            start()
        }       
    }

    @AfterClass
    static void tearDownClass() {
    	component.stop()
    }

    @Test
    void testServiceApplicationConnection() {     
        def request = new Request(Method.GET, serviceAppUrl + "/status")
        def client = new Client(Protocol.HTTP)     
        def response = client.handle(request)
        assertEquals Status.SUCCESS_OK , response.status
    }

    @Test
    void testFeedApplicationConnection() {     
        def request = new Request(Method.GET, feedAppUrl + "/1-init.atom")
        def client = new Client(Protocol.HTTP)     
        def response = client.handle(request)
        assertEquals Status.SUCCESS_OK , response.status        
    }    
    
    @Test
    void testConcurrentCalls() {
    	repoListener.noofConnections = 0
        repoListener.isConcurrent = false
    	
        def request = new Request(Method.POST, "${serviceAppUrl}/collector")
        def param = "feed=${feedAppUrl}/1-init.atom"        
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA)     
        def client = new Client(Protocol.HTTP)     
        def response = client.handle(request)
        assertEquals Status.SUCCESS_OK , response.status                    

        request = new Request(Method.POST, "${serviceAppUrl}/collector")
        param = "feed=${feedAppUrl}/2-updated_t1.atom"        
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA)     
        response = client.handle(request)
        assertEquals Status.SUCCESS_OK , response.status        
        
        // TODO: Allow time for collect to finish. Causes deadlock otherwise, 
        // read feed seems to wait indefinitely for closed feed.
        Thread.sleep(3000)

        assertFalse "Concurrent connections to repository.", repoListener.isConcurrent        
    }
}

/**
 * Listener for checking if concurrent connections are opened to repository.
 */
class RListener extends RepoConnectionListener implements RepositoryListener {

	 def noofConnections = 0
	 def isConcurrent = false
	
	void initialize(Repository repo) { }
    void setDataDir(Repository repo, File dataDir) { }
    void shutDown(Repository repo) { }   

    void getConnection(Repository repo, RepositoryConnection conn) {
    	noofConnections++
    	isConcurrent = isConcurrent | noofConnections > 1 
    }

	@Override
    void close(RepositoryConnection conn) {
		noofConnections--
    }
}
