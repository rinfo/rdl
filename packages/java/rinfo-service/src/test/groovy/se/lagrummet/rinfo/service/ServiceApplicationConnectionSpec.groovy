package se.lagrummet.rinfo.service

import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Client
import org.restlet.Component
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Status
import org.restlet./*routing.*/VirtualHost

import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.event.RepositoryListener;
import se.lagrummet.rinfo.service.util.FeedApplication
import se.lagrummet.rinfo.service.util.RepoConnectionListener
import spock.lang.Specification

/**
 * Test for verifying ServiceApplication's handling of multiple connections.
 */
class ServiceApplicationConnectionSpec extends Specification {

    static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service-test.properties"

    static serviceAppUrl
    static feedAppUrl
    static repoListener
    static component

    def setupSpec() {
        def config = new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME)
        def serviceAppPort = config.getInt("test.serviceAppPort")
        def feedAppPort = config.getInt("test.feedAppPort")
        def appUrlBase = config.getString("test.appUrlBase")
        serviceAppUrl = appUrlBase + ":" + serviceAppPort
        feedAppUrl = appUrlBase + ":" + feedAppPort

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

    def cleanupSpec() {
        component.stop()
    }

    def testServiceApplicationConnection() {
        def request = new Request(Method.GET, serviceAppUrl + "/status")
        def client = new Client(Protocol.HTTP)
        def response = client.handle(request)
        
        expect:
        response.status == Status.SUCCESS_OK
    }

    def testFeedApplicationConnection() {
        def request = new Request(Method.GET, feedAppUrl + "/1-init.atom")
        def client = new Client(Protocol.HTTP)
        def response = client.handle(request)
        
        expect:
        response.status == Status.SUCCESS_OK
    }

    def testConcurrentCalls() {
        when:
        repoListener.noofConnections = 0
        repoListener.isConcurrent = false

        def request = new Request(Method.POST, "${serviceAppUrl}/collector")
        def param = "feed=${feedAppUrl}/1-init.atom"
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA)
        def client = new Client(Protocol.HTTP)
        def response = client.handle(request)
        
        then:
        response.status == Status.SUCCESS_OK
        
        when:
        request = new Request(Method.POST, "${serviceAppUrl}/collector")
        param = "feed=${feedAppUrl}/2-updated_t1.atom"
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA)
        response = client.handle(request)
        
        then:
        response.status == Status.SUCCESS_OK

        // TODO: Allow time for collect to finish. Causes deadlock otherwise,
        // read feed seems to wait indefinitely for closed feed.
        Thread.sleep(3000)

        expect:
        !repoListener.isConcurrent
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

    @Override
    void getConnection(Repository repo, RepositoryConnection conn) {
        noofConnections++
        isConcurrent = isConcurrent | noofConnections > 1
    }

    @Override
    void close(RepositoryConnection conn) {
        noofConnections--
    }
}
