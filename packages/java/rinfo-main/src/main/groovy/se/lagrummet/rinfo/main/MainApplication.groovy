package se.lagrummet.rinfo.main

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

    private FileDepot depot
    private CollectorRunner collectorRunner

    // TODO: depot and collectorRunner should bootstrap themselves..
    MainApplication(Context parentContext,
            FileDepot depot, CollectorRunner collectorRunner) {
        super(parentContext)
        this.depot = depot
        this.collectorRunner = collectorRunner
    }

    @Override
    synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attach("/collector",
                new CollectorRestlet(context, depot, collectorRunner))
        router.attachDefault(new DepotFinder(context, depot))
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


class CollectorRestlet extends Restlet {

    static final ALLOWED = new HashSet([Method.GET])
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
