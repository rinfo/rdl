package se.lagrummet.rinfo.main

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.InputRepresentation
import org.restlet.resource.Representation
import org.restlet.resource.Resource
import org.restlet.resource.ResourceException
import org.restlet.resource.StringRepresentation
import org.restlet.resource.Variant

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed

import se.lagrummet.rinfo.main.storage.CollectorLog


class CollectorLogResource extends Resource {

    private String contextPath
    private CollectorLog collectorLog

    public CollectorLogResource(Context context, Request request, Response response) {
        super(context, request, response)
        collectorLog = ContextAccess.getCollectorLog(context)
        // ".../{contextPath}"
        //contextPath = (String) getRequest().getAttributes().get("contextPath")
        //getVariants().add(new Variant(MediaType.APPLICATION_RDF_XML))
        getVariants().add(new Variant(MediaType.APPLICATION_ATOM_XML))
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        def logSession = collectorLog.openSession()
        if (MediaType.APPLICATION_ATOM_XML.equals(variant.getMediaType())) {
            Feed feed = Abdera.getInstance().newFeed()
            try {
                for (collect in logSession.describer.ofType("rc:Collect")) {
                    //if (collect.getRel("iana:self").equals(collect.getRel("iana:current")))
                    Entry entry = feed.addEntry()
                    entry.setUpdated(collect.getNative("tl:end"))
                    entry.setPublished(collect.getNative("tl:start"))
                    entry.setId(collect.getAbout())//.replace(' ', '+'))
                }
            } finally {
                logSession.close()
            }
            def byteOut = new ByteArrayOutputStream()
            feed.writeTo("prettyxml", byteOut)
            return new InputRepresentation(
                    new ByteArrayInputStream(byteOut.toByteArray()),
                    MediaType.APPLICATION_ATOM_XML)
        }
        return null
    }

}
