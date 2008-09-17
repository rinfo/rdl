package se.lagrummet.rinfo.main

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Application
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.Router
import org.restlet.data.CharacterSet
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.supply.DepotFinder
import se.lagrummet.rinfo.collector.CollectorRunner


class MainApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-main.properties"

    private FileDepot depot
    private CollectorRunner collectorRunner

    MainApplication(Context parentContext) {
        def config = new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME)
        this(parentContext, config)
    }

    MainApplication(Context parentContext, AbstractConfiguration config) {
        super(parentContext)
        depot = FileDepot.newConfigured(config)
        collectorRunner = new CollectorRunner(depot, null)
        collectorRunner.configure(config)
    }

    @Override
    synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attach("/collector",
                new CollectorRestlet(getContext(), depot, collectorRunner))
        router.attachDefault(new DepotFinder(getContext(), depot))
        return router
    }

    @Override
    public void start() {
        super.start()
        collectorRunner.startup()
    }

    @Override
    public void stop() {
        super.stop()
        collectorRunner.shutdown()
    }

}


// FIXME: rebuild (as Finder+Handler)
class CollectorRestlet extends Restlet {

    static final ALLOWED = new HashSet([Method.GET]) // TODO: only POST
    private FileDepot depot
    private CollectorRunner collectorRunner

    public CollectorRestlet(Context context, FileDepot depot, CollectorRunner collectorRunner) {
        super(context)
        this.depot = depot
        this.collectorRunner = collectorRunner
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
        collectorRunner.spawnOneFeedCollect([new URL(feedUrl)])
        response.setEntity("Scheduled collect of <${feedUrl}>.", MediaType.TEXT_PLAIN)
    }

}
