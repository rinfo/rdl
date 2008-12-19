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

import se.lagrummet.rinfo.collector.NotAllowedSourceFeedException


class MainApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-main.properties"
    public static final String COLLECTOR_RUNNER_CONTEXT_KEY =
            "rinfo.main.collector.restlet.context"

    private FeedCollectScheduler collectScheduler
    private FileDepot depot

    MainApplication(Context context) {
        this(context, new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME))
    }

    MainApplication(Context context, AbstractConfiguration config) {
        super(context)
        depot = FileDepot.newConfigured(config)
        collectScheduler = new FeedCollectScheduler(depot, null, config)
        collectScheduler.batchCompletedCallback = new FeedUpdatePingNotifyer(
                new URL(config.getString("rinfo.main.publicSubscriptionFeed")),
                config.getList("rinfo.main.collector.onCompletePingTargets").
                    collect { new URL(it) }
            )
        getContext().getAttributes().putIfAbsent(
                COLLECTOR_RUNNER_CONTEXT_KEY, collectScheduler)
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
        collectScheduler.startup()
    }

    @Override
    public void stop() {
        super.stop()
        collectScheduler.shutdown()
    }

}


class CollectorHandler extends Handler {

    static String BAD_MSG = "Requires POST and a feed query parameter (URL)."

    @Override
    public boolean allowPost() { return true; }

    @Override
    public void handleGet() {
        // TODO: some form of collect status page..
        getResponse().setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED, BAD_MSG)
    }

    @Override
    public void handlePost() {
        // TODO: verify source of request (or only via loadScheduler.sourceFeedUrls)?
        // TODO: error handling.. (report and/or (public) log)

        def collectScheduler = (FeedCollectScheduler) context.getAttributes().get(
                MainApplication.COLLECTOR_RUNNER_CONTEXT_KEY)

        String feedUrl = request.getEntityAsForm().getFirstValue("feed")
        if (feedUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, BAD_MSG)
            return
        }

        def msg = "Scheduled collect of <${feedUrl}>."
        def status = null

        try {
            boolean wasScheduled = collectScheduler.triggerFeedCollect(new URL(feedUrl))
            if (!wasScheduled) {
                msg = "The url <${feedUrl}> is already scheduled for collect."
            }
        } catch (NotAllowedSourceFeedException e) {
                msg = "The url <${feedUrl}> is not an allowed source feed."
                status = Status.CLIENT_ERROR_FORBIDDEN
        }

        if (status != null) {
            getResponse().setStatus(status)
        }
        getResponse().setEntity(msg, MediaType.TEXT_PLAIN)
    }

}
