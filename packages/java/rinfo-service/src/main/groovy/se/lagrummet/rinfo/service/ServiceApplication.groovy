package se.lagrummet.rinfo.service

import org.restlet.Application
import org.restlet.Context
import org.restlet.Request
import org.restlet.Response
import org.restlet.Restlet
import org.restlet.data.Form
import org.restlet.data.MediaType
import org.restlet.resource.Directory
import org.restlet.resource.Finder
import org.restlet.routing.Redirector
import org.restlet.routing.Router
import org.restlet.routing.Variable


class ServiceApplication extends Application {

    static final String CONFIG_PROPERTIES_FILE_NAME = "rinfo-service.properties"
    static final String COMMON_CONFIG_PROPERTIES_FILE_NAME = "/etc/rinfo/"+CONFIG_PROPERTIES_FILE_NAME

    static final String SERVICE_COMPONENTS_CONTEXT_KEY =
            "rinfo.service.components.restlet.context"

    ServiceComponents components

    String mediaDirUrl = "war:///"

    boolean allowCORS = true

    ServiceApplication(Context parentContext, boolean useCommon = true) {
        super(parentContext)
        setupExtensions()
        components = new ServiceComponents(useCommon&&new File(COMMON_CONFIG_PROPERTIES_FILE_NAME).exists()?COMMON_CONFIG_PROPERTIES_FILE_NAME:CONFIG_PROPERTIES_FILE_NAME)
        getContext().getAttributes().putIfAbsent(SERVICE_COMPONENTS_CONTEXT_KEY, components)
    }

    @Override
    synchronized Restlet createInboundRoot() {
        def ctx = getContext()
        def router = new Router(ctx) {
            @Override
            void handle(Request request, Response response) {
                if (allowCORS) {
                    addCORSHeaders(response)
                }
                super.handle(request, response)
            }
            private addCORSHeaders(Response response) {
                def responseHeaders = response.attributes.get("org.restlet.http.headers")
                if (responseHeaders == null) {
                    responseHeaders = new Form()
                    response.attributes.put("org.restlet.http.headers", responseHeaders)
                }
                responseHeaders.add("Access-Control-Allow-Origin", "*")
                responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                responseHeaders.add("Access-Control-Allow-Credentials", "false")
            }
        }

        router.attach("/",
                new Redirector(ctx, "{rh}/ui/", Redirector.MODE_CLIENT_SEE_OTHER))
        router.attach("/ui",
                new Redirector(ctx, "{rh}/ui/", Redirector.MODE_CLIENT_SEE_OTHER))

        router.attach("/collector", new Finder(ctx, RDFLoaderHandler))

        router.attach("/var/terms",
                new DataFinder(ctx, components.repository,
                    components.jsonLdSettings, components.dataAppBaseUri + "var/terms",
                    "/sparql/construct_terms.rq"))

        router.attach("/var/common",
                new DataFinder(ctx, components.repository,
                    components.jsonLdSettings, components.dataAppBaseUri + "var/common",
                    "/sparql/construct_common.rq"))

        router.attach("/{path}/data",
                new DataFinder(ctx, components.repository,
                    components.jsonLdSettings, components.dataAppBaseUri,
                    "/sparql/construct_relrev_data.rq")
            ).template.variables.put("path", new Variable(Variable.TYPE_URI_PATH))

        router.attach("/{path}/index",
                new DataFinder(ctx, components.repository,
                    components.jsonLdSettings, components.dataAppBaseUri,
                    "/sparql/construct_summary.rq")
            ).template.variables.put("path", new Variable(Variable.TYPE_URI_PATH))

        if (components.elasticQuery && components.simpleElasticQuery) {
            router.attach("/-/{docType}",
                    new ElasticFinder(ctx, components.elasticQuery, components.simpleElasticQuery))
        }

        if (mediaDirUrl) {
            router.attach("/json-ld/", new Directory(ctx, "clap:///json-ld/"))
            router.attach("/css/", new Directory(ctx, mediaDirUrl + "css/"))
            router.attach("/img/", new Directory(ctx, mediaDirUrl + "img/"))
            router.attach("/js/", new Directory(ctx, mediaDirUrl + "js/"))
            router.attach("/ui", new Directory(ctx, mediaDirUrl + "ui"))
        }
        return router
    }

    @Override
    void start() {
        super.start()
        components.startup()
    }

    @Override
    void stop() {
        super.stop()
        components.shutdown()
    }

    void setupExtensions() {
        tunnelService.extensionsTunnel = true
        metadataService.addExtension("ttl", MediaType.APPLICATION_RDF_TURTLE)
    }

}
