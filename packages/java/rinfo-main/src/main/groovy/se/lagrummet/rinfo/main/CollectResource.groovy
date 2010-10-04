package se.lagrummet.rinfo.main

import org.restlet.Context
import org.restlet.data.MediaType
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.resource.InputRepresentation
import org.restlet.resource.Representation
import org.restlet.resource.StringRepresentation
import org.restlet.resource.Resource
import org.restlet.resource.ResourceException
import org.restlet.resource.Variant

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.main.storage.CollectorLog


class CollectResource extends Resource {

    private CollectorLog collectorLog
    private String collectContextUri

    public CollectResource(Context context, Request request, Response response) {
        super(context, request, response)
        collectorLog = ContextAccess.getCollectorLog(context)
        def feedIdAtDateTime = request.getAttributes().get("feedIdAtDateTime")
        if (feedIdAtDateTime != null) {
            collectContextUri = "http://rinfo.lagrummet.se" +
                    request.getResourceRef().getPath().toString()
                    //"${collectorLog.systemBaseUri}log/collect/${feedIdAtDateTime}"
            getVariants().add(new Variant(MediaType.APPLICATION_RDF_XML))
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (MediaType.APPLICATION_RDF_XML.equals(variant.getMediaType())) {
            def context = new org.openrdf.model.impl.URIImpl(collectContextUri)
            // TODO: return null if context is not found
            def ins = RDFUtil.toInputStream(collectorLog.repo,
                    "application/rdf+xml", true, context)
            return new InputRepresentation(ins, MediaType.APPLICATION_RDF_XML)
        }
    }

}
