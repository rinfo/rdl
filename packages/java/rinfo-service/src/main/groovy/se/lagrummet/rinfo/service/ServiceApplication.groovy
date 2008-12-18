package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory
import org.slf4j.Logger

import org.restlet.Application
import org.restlet.Context
import org.restlet.Directory
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
import org.restlet.resource.Representation
import org.restlet.resource.Resource
import org.restlet.resource.StringRepresentation
import org.restlet.resource.Variant

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration

import se.lagrummet.rinfo.collector.NotAllowedSourceFeedException


class ServiceApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service.properties"
    public static final String RDF_LOADER_CONTEXT_KEY =
            "rinfo.service.rdfloader.restlet.context"

    private SesameLoadScheduler loadScheduler

    public ServiceApplication(Context parentContext) {
        super(parentContext)
        def config = new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME)
        loadScheduler = new SesameLoadScheduler(config)
        def attrs = getContext().getAttributes()
        attrs.putIfAbsent(RDF_LOADER_CONTEXT_KEY, loadScheduler)
    }

    @Override
    public synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attach("/collector", new Finder(getContext(), RDFLoaderHandler))
        // FIXME: copy to resources and point out in config.
        def treeDir = new File("../../../laboratory/services/SparqlToAtom/")
        router.attach("/view", new SparqlTreeRouter(getContext(), treeDir))
        // FIXME:? How to let through to webapp dir instead (if desirable)?
        router.attach("/css", new Directory(getContext(),
                new File("src/main/webapp/css").toURI().toString()))
        //TODO:? router.attach("/spec", new Directory(getContext(), ".../documents/acceptance"))
        router.attach("/status", StatusResource)
        return router
    }

    @Override
    public void stop() {
        super.stop()
        loadScheduler.shutdown()
    }

}


class RDFLoaderHandler extends Handler {

    @Override
    public boolean allowPost() { return true; }

    @Override
    public void handlePost() {
        // TODO: verify source of request (or only via loadScheduler.sourceFeedUrls)?
        // TODO: error handling.. (report and/or (public) status/log)

        def loadScheduler = (SesameLoadScheduler) getContext().getAttributes().get(
                ServiceApplication.RDF_LOADER_CONTEXT_KEY)

        String feedUrl = request.getEntityAsForm().getFirstValue("feed")
        if (feedUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing feed parameter.")
            return
        }

        def msg = "Scheduled collect of <${feedUrl}>."
        def status = null

        try {
            boolean wasScheduled = loadScheduler.triggerFeedCollect(new URL(feedUrl))
            if (!wasScheduled) {
                msg = "The service is busy collecting data."
                status = Status.SERVER_ERROR_SERVICE_UNAVAILABLE
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

/*
 *  Basic resource for simple status message.
 *
 *  TODO: replace this by a handleGet in RDFLoaderHandler?
 *  TODO: some form of collect status page..?
 */
class StatusResource extends Resource {
    public StatusResource(Context context, Request request, Response response) {
        super(context, request, response)
        getVariants().add(new Variant(MediaType.TEXT_PLAIN))
    }
    @Override
    public Representation getRepresentation(Variant variant) {
        def representation = new StringRepresentation("OK", MediaType.TEXT_PLAIN)
        return representation
    }
}
