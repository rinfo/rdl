package se.lagrummet.rinfo.main

import org.restlet.Context
import org.restlet.data.MediaType
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.resource.InputRepresentation
import org.restlet.resource.Representation
import org.restlet.resource.Resource
import org.restlet.resource.ResourceException
import org.restlet.resource.Variant

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed

import se.lagrummet.rinfo.main.storage.CollectorLog


class LogListResource extends Resource {

    private CollectorLog collectorLog

    public LogListResource(Context context, Request request, Response response) {
        super(context, request, response)
        collectorLog = ContextAccess.getCollectorLog(context)
        getVariants().add(new Variant(MediaType.APPLICATION_ATOM_XML))
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (MediaType.APPLICATION_ATOM_XML.equals(variant.getMediaType())) {
            // TODO:? not session, just get a prefix-preset describer...
            def logSession = collectorLog.openSession()
            Feed feed = Abdera.getInstance().newFeed()

            feed.setId(collectorLog.systemBaseUri + "log")
            try {
                for (collect in logSession.newDescriber().ofType("rc:Collect")) {
                    Entry entry = feed.insertEntry()
                    entry.setUpdated(collect.getNative("tl:end"))
                    entry.setPublished(collect.getNative("tl:start"))
                    entry.setId(collect.getAbout())
                    entry.setTitle(new URI(collect.getAbout()).getPath())
                    def link = entry.addLink(new URI(collect.getAbout()).getPath(),
                            "alternate", "application/rdf+xml", null, null, -1)
                    for (via in collect.getRels("iana:via")) {
                        //if (via.getRel("iana:self").equals(via.getRel("iana:current")))
                        def enclosure = entry.addLink(new URI(via.getAbout()).getPath(),
                                "enclosure", "application/rdf+xml", null, null, -1)
                        enclosure.setAttributeValue("modified", via.getValue("awol:updated"))
                    }
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
