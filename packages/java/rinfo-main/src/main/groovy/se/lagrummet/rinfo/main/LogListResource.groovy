package se.lagrummet.rinfo.main

import org.restlet.Context
import org.restlet.data.MediaType
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.InputRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.Resource
import org.restlet.resource.ResourceException
import org.restlet.representation.Variant

import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.main.storage.CollectorLog


class LogListResource extends Resource {

    private CollectorLog collectorLog

    static final CONSTRUCT_INDEX_RQ = """
        PREFIX iana: <http://www.iana.org/assignments/relation/>
        PREFIX rc: <http://rinfo.lagrummet.se/ns/2008/10/collector#>

        CONSTRUCT WHERE {
            ?rc a rc:Collect;
                ?rcprop ?rcvalue .
            ?rc iana:via ?via .
            ?via ?viaprop ?viavalue .
        } """

    public LogListResource(Context context, Request request, Response response) {
        super(context, request, response)
        collectorLog = ContextAccess.getCollectorLog(context)
        getVariants().add(new Variant(MediaType.APPLICATION_RDF_XML))
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        def conn = collectorLog.repo.getConnection()
        def outRepo = null
        try {
            outRepo = RDFUtil.constructQuery(conn, CONSTRUCT_INDEX_RQ)
        } finally {
            conn.close()
        }
        if (outRepo == null) {
            return null
        }
        def ins = RDFUtil.toInputStream(outRepo, "application/rdf+xml", false)
        if (MediaType.APPLICATION_RDF_XML.equals(variant.getMediaType())) {
            return new InputRepresentation(ins, MediaType.APPLICATION_RDF_XML)
        } else {
            return null
        }
    }

}
