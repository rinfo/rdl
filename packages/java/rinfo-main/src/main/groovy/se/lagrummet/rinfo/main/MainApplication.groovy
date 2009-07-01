package se.lagrummet.rinfo.main

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration

import org.restlet.Application
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet./*resource.*/Finder
import org.restlet./*routing.*/Router

import se.lagrummet.rinfo.store.supply.DepotFinder


class MainApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-main.properties"
    public static final String COLLECTOR_RUNNER_CONTEXT_KEY =
            "rinfo.main.collector.restlet.context"

    Components components

    MainApplication(Context context) {
        this(context, new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME))
    }

    MainApplication(Context context, Configuration config) {
        super(context)
        components = new Components(config)
        getContext().getAttributes().putIfAbsent(
                COLLECTOR_RUNNER_CONTEXT_KEY, components.getCollectScheduler())
    }

    @Override
    synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attach("/collector",
                new Finder(getContext(), CollectorHandler))
        router.attachDefault(
                new DepotFinder(getContext(), components.getStorage().getDepot()))
        return router
    }

    @Override
    public void start() {
        super.start()
        components.startup()
    }

    @Override
    public void stop() {
        super.stop()
        components.shutdown()
    }

}
