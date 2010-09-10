package integration.rinfo.main

import org.apache.commons.io.FileUtils
import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Application
import org.restlet.Client
import org.restlet.Component
import org.restlet.Restlet
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.data.Request
import static org.restlet.data.Status.CLIENT_ERROR_FORBIDDEN
import static org.restlet.data.Status.SUCCESS_ACCEPTED

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.supply.DepotFinder

import se.lagrummet.rinfo.main.MainApplication

import spock.lang.*


class MainApplicationIT extends Specification {

    @Shared tempDirs = []
    @Shared webapps = [:]

    @Shared String subscriptionFeed
    @Shared String adminUrl
    @Shared String sourceUrl

    def setupSpec() {
        startWebApp "main", 8480, { new MainApplication(it) }

        def mainCfg = webapps.main.app.components.config
        subscriptionFeed = mainCfg.getProperty('rinfo.main.publicSubscriptionFeed')
        adminUrl = mainCfg.getProperty('rinfo.main.collector.adminFeedUrl')
        //mainCfg.getProperty('rinfo.main.storage.sourceFeedsEntryId')
        sourceUrl = localhost(8680, "/feed/current")
        startDepotApp "admin", 8580
        startDepotApp "testsource", 8680
    }

    def cleanupSpec() {
        webapps.each { name, webapp ->
            println "Stopping webapp: ${name} ..."
            webapp.component.stop()
        }
        tempDirs.each {
            println "Deleting temp dir: ${it} ..."
            FileUtils.forceDelete(it)
        }
        println "Done."
    }


    def "pings should be handled"() {
        expect:
        ping(localhost(8480, "/collector"), adminUrl).status == SUCCESS_ACCEPTED
        ping(localhost(8480, "/collector"), sourceUrl).status == CLIENT_ERROR_FORBIDDEN
    }


    private localhost(port, path) { "http://localhost:${port}${path}" }

    private startWebApp(name, int port, Closure makeApp) {
        def component = new Component()
        component.servers.add(Protocol.HTTP, port)
        def app = makeApp(component.context.createChildContext())
        component.defaultHost.attach(app)
        component.start()
        webapps[name] = [app: app, component: component]
    }

    def startDepotApp(name, port) {
        startWebApp name, port, {
            new DepotApplication(it, "http://${name}", tempDir(name))
        }
    }

    private ping(reciever, feedUrl) {
        def request = new Request(Method.POST, reciever)
        def feedUrlMsg = "feed=${feedUrl}"
        request.setEntity(feedUrlMsg, MediaType.MULTIPART_FORM_DATA)
        def client = new Client(Protocol.HTTP)
        println "Pinging ${reciever} with ${feedUrl}..."
        return client.handle(request)
    }

    private tempDir(name) {
        def dir = File.createTempFile("rinfo-${name}-", "",
                new File(System.getProperty("java.io.tmpdir")))
        assert dir.delete(); assert dir.mkdir()
        tempDirs << dir
        return dir
    }

}


class DepotApplication extends Application {
    def depot
    DepotApplication(parentContext, uri, depotDir) {
        super(parentContext)
        depot = new FileDepot(new URI(uri), depotDir)
        depot.atomizer.feedPath = "/feed"
    }
    synchronized Restlet createRoot() {
        return new DepotFinder(context, depot)
    }
}


