package se.lagrummet.rinfo.service

import org.junit.*
import static org.junit.Assert.*

import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Client
import org.restlet.Component
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status
import org.restlet./*routing.*/VirtualHost

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.rdf.repo.RepositoryHandler
import se.lagrummet.rinfo.service.util.FeedApplication


/**
 * Test for verifying that a ping to the ServiceApplication leads to reading
 * the feed and fetching its RDF metadata.
 *
 * Cleans repository before and after testing.
 */
class ServiceApplicationTest {

    static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service-test.properties"

    static serviceAppUrl
    static serviceAppPort
    static feedAppUrl
    static feedAppPort
    //static sesameRepoPath
    static component

    static RepositoryHandler repositoryHandler

    @BeforeClass
    static void setupClass() {
        def config = new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME)
        serviceAppPort = config.getInt("test.serviceAppPort")
        feedAppPort = config.getInt("test.feedAppPort")
        def appUrlBase = config.getString("test.appUrlBase")
        serviceAppUrl = appUrlBase + ":" + serviceAppPort
        feedAppUrl = appUrlBase + ":" + feedAppPort
        //sesameRepoPath = config.getString("rinfo.service.sesameRepoPath")

        //void startComponent() {
        component = new Component()
        def context = component.getContext()

        // create a local ServiceApplication
        def serviceApplication = new ServiceApplication(context.createChildContext())
        repositoryHandler = serviceApplication.repositoryHandler
        repositoryHandler.cleanRepository()

        // create a local application that serves feeds
        def feedApplication = new FeedApplication(context.createChildContext())

        // start applications
        def feedHost = new VirtualHost(context.createChildContext())
        feedHost.setHostPort("" + feedAppPort)
        feedHost.attach(feedApplication)

        def serviceHost = new VirtualHost(context.createChildContext())
        serviceHost.setHostPort("" + serviceAppPort)
        serviceHost.attach(serviceApplication)

        component.servers.add(Protocol.HTTP, serviceAppPort)
        component.servers.add(Protocol.HTTP, feedAppPort)
        component.clients.add(Protocol.HTTP)
        component.clients.add(Protocol.FILE)
        component.hosts.add(serviceHost)
        component.hosts.add(feedHost)
        component.start()
    }

    @AfterClass
    static void stopComponent() {
        repositoryHandler.cleanRepository()
        component.stop()
    }

    @Test
    void testReadMetaFromFeed() {

        // add metadata
        def request = new Request(Method.POST, "${serviceAppUrl}/collector")
        def param = "feed=${feedAppUrl}/1-init.atom"
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA)
        def client = new Client(Protocol.HTTP)
        def response = client.handle(request)
        assertEquals Status.SUCCESS_OK , response.status

        Thread.sleep(2000)

        assertEquals 2, countContexts()
    }

    int countContexts() {
        def conn = repositoryHandler.repository.connection
        def res = conn.contextIDs
        def i = res.asList().size()
        res.close()
        conn.close()
        return i
    }

}
