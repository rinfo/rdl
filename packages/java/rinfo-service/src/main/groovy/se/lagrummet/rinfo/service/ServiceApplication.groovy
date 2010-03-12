package se.lagrummet.rinfo.service

import org.slf4j.LoggerFactory
import org.slf4j.Logger

import org.restlet.Application
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.CharacterSet
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status
import org.restlet.resource./*.representation.*/Representation
import org.restlet.resource./*.representation.*/StringRepresentation
import org.restlet.resource./*.representation.*/Variant
import org.restlet./*resource.*/Directory
import org.restlet./*resource.*/Finder
import org.restlet./*resource.*/Handler
import org.restlet.resource.Resource
import org.restlet./*routing.*/Router

import org.openrdf.repository.Repository

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration

import se.lagrummet.rinfo.collector.NotAllowedSourceFeedException
import se.lagrummet.rinfo.rdf.repo.RepositoryHandler
import se.lagrummet.rinfo.rdf.repo.RepositoryHandlerFactory


class ServiceApplication extends Application {

    public static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service.properties"
    public static final String REPO_PROPERTIES_SUBSET_KEY = "rinfo.service.repo"

    public static final String RDF_LOADER_CONTEXT_KEY =
            "rinfo.service.rdfloader.restlet.context"

    String mediaDirUrl = "war:///css/"

    SesameLoadScheduler loadScheduler
    RepositoryHandler repositoryHandler

    public ServiceApplication(Context parentContext) {
        super(parentContext)
        // TODO: reuse IoC pattern from main (Components etc.)
        def config = new PropertiesConfiguration(CONFIG_PROPERTIES_FILE_NAME)

        repositoryHandler = RepositoryHandlerFactory.create(config.subset(
                REPO_PROPERTIES_SUBSET_KEY))
        repositoryHandler.initialize()

        loadScheduler = new SesameLoadScheduler(config, repositoryHandler.repository)
        def attrs = getContext().getAttributes()
        attrs.putIfAbsent(RDF_LOADER_CONTEXT_KEY, loadScheduler)
    }

    @Override
    public synchronized Restlet createRoot() {
        def router = new Router(getContext())
        router.attach("/status", StatusResource)
        router.attach("/collector", new Finder(getContext(), RDFLoaderHandler))
        router.attach("/rdata", new SparqlTreeRouter(
                getContext(), repositoryHandler.repository))
        if (mediaDirUrl) {
            router.attach("/css", new Directory(getContext(), mediaDirUrl))
        }
        return router
    }

    @Override
    public void start() {
        super.start()
        loadScheduler.startup()
    }

    @Override
    public void stop() {
        super.stop()
        loadScheduler.shutdown()
        repositoryHandler.shutDown()
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
