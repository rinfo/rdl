package se.lagrummet.rinfo.main

import org.restlet.Context
import org.restlet.data.MediaType
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.InputRepresentation
import org.restlet.representation.Representation
import org.restlet.representation.WriterRepresentation
import org.restlet.resource.Resource
import org.restlet.resource.ResourceException
import org.restlet.representation.Variant

import se.lagrummet.rinfo.base.rdf.GritTransformer
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.main.storage.CollectorLog


class CollectResource extends Resource {

    private String collectContextUri
    private CollectorLog collectorLog
    private GritTransformer logToXhtml

    public CollectResource(Context context, Request request, Response response) {
        super(context, request, response)
        collectorLog = ContextAccess.getCollectorLog(context)
        logToXhtml = ContextAccess.getLogToXhtml(getContext())
        def contextPath = request.getAttributes().get("contextPath")
        logger.info("contextPath = <${contextPath}>")
        if (contextPath != null) {
            collectContextUri = "${collectorLog.systemBaseUri}log/collect/${contextPath}"
                    //"http://rinfo.lagrummet.se" +
                    //request.getResourceRef().getPath().toString()
                    //"/"+request.getResourceRef().getRelativeRef().getPath().toString()
            getVariants().add(new Variant(MediaType.APPLICATION_RDF_XML))
            getVariants().add(new Variant(MediaType.TEXT_HTML))
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        logger.info("Representing <${collectContextUri}> as RDF")
        def context = new org.openrdf.model.impl.URIImpl(collectContextUri)
        // TODO: return null if context is not found
        def ins = RDFUtil.toInputStream(collectorLog.repo,
                "application/rdf+xml", true, context)

        if (MediaType.APPLICATION_RDF_XML.equals(variant.getMediaType())) {
            return new InputRepresentation(ins, MediaType.APPLICATION_RDF_XML)

        } else if (MediaType.TEXT_HTML.equals(variant.getMediaType())) {
            return new WriterRepresentation(MediaType.TEXT_HTML) {
                public void write(Writer writer) throws IOException {
                    try {
                        logToXhtml.writeXhtml(ins, writer);
                    } finally {
                        ins.close();
                    }
                }
            }
        }
    }

}
