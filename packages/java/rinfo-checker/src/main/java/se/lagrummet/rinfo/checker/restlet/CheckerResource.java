package se.lagrummet.rinfo.checker.restlet;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;

import org.openrdf.repository.Repository;

import org.restlet.*;
import org.restlet.data.*;
import org.restlet.resource.InputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import se.lagrummet.rinfo.base.rdf.RDFUtil;

import se.lagrummet.rinfo.main.storage.StorageHandler;

import se.lagrummet.rinfo.checker.Checker;


public class CheckerResource extends Resource {

    Templates gritXslt;
    Templates logXslt;

    public CheckerResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_HTML));
        // TODO: get xslts from context (don't reload on each request)
        try {
            gritXslt = TransformerUtil.saxTf.newTemplates(new StreamSource(
                        getClass().getResourceAsStream(
                            "/xslt/rdfxml-grit.xslt")));
            // TODO: set mediabase param
            logXslt = TransformerUtil.saxTf.newTemplates(new StreamSource(
                        getClass().getResourceAsStream(
                            "/xslt/collector_log.xslt")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        return new InputRepresentation(
                getClass().getResourceAsStream("/xhtml/index.xhtml"), MediaType.TEXT_HTML);
    }

    @Override public boolean allowPost() { return true; }

    @Override
    public void handlePost() {
        try {
            Form form = getRequest().getEntityAsForm();
            String feedUrl = form.getFirstValue("feedUrl");
            String maxEntriesStr = form.getFirstValue("maxEntries");
            int maxEntries = !StringUtils.isEmpty(maxEntriesStr) ? Integer.parseInt(maxEntriesStr) : -1;
            List<StorageHandler> handlers =
                    (List<StorageHandler>) getContext().getAttributes().get("handlers");
            Checker checker = new Checker();
            checker.setMaxEntries(maxEntries);
            checker.setHandlers(handlers);
            try {
                Repository logRepo = checker.checkFeed(feedUrl);
                InputStream ins = RDFUtil.toInputStream(logRepo, "application/rdf+xml", true);
                String html = TransformerUtil.toXhtml(ins, gritXslt, logXslt);
                getResponse().setEntity(new StringRepresentation(html, MediaType.TEXT_HTML));
            } finally {
                checker.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: put in rinfo-base?
    public static class TransformerUtil {

        static SAXTransformerFactory saxTf =
                (SAXTransformerFactory) TransformerFactory.newInstance();

        static String toXhtml(InputStream inputStream, Templates... xslts) throws Exception {
            XMLFilter filter = null;
            for (Templates xslt : xslts) {
                XMLFilter nextFilter = saxTf.newXMLFilter(xslt);
                if (filter != null) nextFilter.setParent(filter);
                filter = nextFilter;
            }
            SAXSource transformSource = new SAXSource(filter, new InputSource(inputStream));
            Transformer htmlTransformer = saxTf.newTransformer();
            htmlTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
            htmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            htmlTransformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
                    "-//W3C//DTD XHTML 1.0 Strict//EN");
            htmlTransformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
            StringWriter strWriter = new StringWriter();
            htmlTransformer.transform(transformSource, new StreamResult(strWriter));
            return strWriter.toString();
        }

    }

}
