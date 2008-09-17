package se.lagrummet.rinfo.main

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration

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

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.supply.DepotFinder
import se.lagrummet.rinfo.collector.CollectorRunner


class MainApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-main.properties"
    public static final String DEPOT_CONTEXT_KEY =
            "rinfo.depot.restlet.context"
    public static final String COLLECTOR_RUNNER_CONTEXT_KEY =
            "rinfo.collector.restlet.context"

    private FileDepot depot
    private CollectorRunner collectorRunner

    MainApplication(Context context) {
        this(context, new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME))
    }

    MainApplication(Context context, AbstractConfiguration config) {
        super(context)
        depot = FileDepot.newConfigured(config)
        collectorRunner = new CollectorRunner(depot, null)
        collectorRunner.configure(config)
        def attrs = getContext().getAttributes()
        attrs.putIfAbsent(DEPOT_CONTEXT_KEY, depot)
        attrs.putIfAbsent(COLLECTOR_RUNNER_CONTEXT_KEY, collectorRunner)
    }

    @Override
    synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attach("/collector", new Finder(getContext(), CollectorHandler))
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


class CollectorHandler extends Handler {

    static String BAD_MSG = "Requires POST and a feed query parameter (URL)."

    @Override
    public boolean allowPost() { return true; }

    @Override
    public void handleGet() {
        getResponse().setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED, BAD_MSG)
    }

    @Override
    public void handlePost() {
        def attrs = context.getAttributes()
        def depot = (FileDepot) attrs.get(MainApplication.DEPOT_CONTEXT_KEY)
        def collectorRunner = (CollectorRunner) attrs.get(
                MainApplication.COLLECTOR_RUNNER_CONTEXT_KEY)

        String feedUrl = getRequest().getResourceRef().
                getQueryAsForm(CharacterSet.UTF_8).getFirstValue("feed")
        if (feedUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, BAD_MSG)
            return
        }
        collectorRunner.spawnOneFeedCollect(new URL(feedUrl))
        getResponse().setEntity(
                "Scheduled collect of <${feedUrl}>.", MediaType.TEXT_PLAIN)
    }

}
