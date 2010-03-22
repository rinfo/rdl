package se.lagrummet.rinfo.service

import spock.lang.*

import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Client
import org.restlet.Component
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Status
import org.restlet./*routing.*/VirtualHost

import se.lagrummet.rinfo.rdf.repo.RepositoryHandler
import se.lagrummet.rinfo.service.util.FeedApplication


/**
 * Test for verifying that a ping to the ServiceApplication leads to reading
 * the feed and fetching its RDF metadata.
 *
 * Cleans repository before and after testing.
 */
class ServiceApplicationSpec extends Specification {

    static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service-test.properties"

    static def serviceAppUrl
    static def serviceAppPort
    static def feedAppUrl
    static def feedAppPort
    //static sesameRepoPath
    static def component

    static RepositoryHandler repositoryHandler

    def setupSpec() {
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

    def cleanupSpec() {
        repositoryHandler.cleanRepository()
        component.stop()
    }

    def "should read meta from feed"() {
        given:
        countContexts() == 0

        when: "service is pinged with a feed containing entries referencing rdf"
        def request = new Request(Method.POST, "${serviceAppUrl}/collector")
        def param = "feed=${feedAppUrl}/1-init.atom"
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA)
        def client = new Client(Protocol.HTTP)
        def response = client.handle(request)
        Thread.sleep(2000)

        then: "contexts will be created for each entry"
        response.status == Status.SUCCESS_OK
        countContexts() == 2
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
