package se.lagrummet.rinfo.main

import org.restlet.Context
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.Representation
import org.restlet.resource.Resource
import org.restlet.resource.ResourceException
import org.restlet.representation.Variant

import se.lagrummet.rinfo.main.storage.CollectorLog


class CollectResource extends Resource {

    private String collectContextUri
    private CollectorLog collectorLog
    private CollectDataRepresenter representer

    public CollectResource(Context context, Request request, Response response) {
        super(context, request, response)
        collectorLog = ContextAccess.getCollectorLog(context)
        representer = new CollectDataRepresenter(ContextAccess.getLogToXhtml(context))
        def contextPath = request.getAttributes().get("contextPath")
        logger.info("contextPath = <${contextPath}>")
        if (contextPath != null) {
            collectContextUri = "${collectorLog.reportBaseUri}${contextPath}"
            variants.addAll(representer.variants)
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        logger.info("Representing <${collectContextUri}> as RDF")
        return representer.represent(collectorLog.repo, variant, collectContextUri)
    }

}
