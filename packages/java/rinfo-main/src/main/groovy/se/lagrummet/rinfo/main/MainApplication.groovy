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

import se.lagrummet.rinfo.collector.NotAllowedSourceFeedException


class MainApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-main.properties"
    public static final String COLLECTOR_RUNNER_CONTEXT_KEY =
            "rinfo.main.collector.restlet.context"

    private FeedCollectScheduler collectScheduler
    private Storage storage

    MainApplication(Context context) {
        this(context, new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME))
    }

    MainApplication(Context context, Configuration config) {
        super(context)
        storage = new Storage(config)

        URL publicSubscriptionFeed = null
        List<URL> onCompletePingTargets = []
        try {
            publicSubscriptionFeed = new URL(
                    config.getString("rinfo.main.publicSubscriptionFeed"))
            onCompletePingTargets = config.getList(
                    "rinfo.main.collector.onCompletePingTargets").collect { new URL(it) }
        } catch (MalformedURLException e) {
            // TODO: handle or fail on bad url:s for collectScheduler
            logger.error("Malformed URL:s in configuration", e)
        }

        collectScheduler = new FeedCollectScheduler(storage, config)
        collectScheduler.batchCompletedCallback = new FeedUpdatePingNotifyer(
                publicSubscriptionFeed, onCompletePingTargets)
        getContext().getAttributes().putIfAbsent(
                COLLECTOR_RUNNER_CONTEXT_KEY, collectScheduler)
    }

    @Override
    synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attach("/collector", new Finder(getContext(), CollectorHandler))
        router.attachDefault(new DepotFinder(getContext(), storage.getDepot()))
        return router
    }

    @Override
    public void start() {
        super.start()
        collectScheduler.startup()
    }

    @Override
    public void stop() {
        super.stop()
        try {
            collectScheduler.shutdown()
        } finally {
            storage.shutdown()
        }
    }

}
