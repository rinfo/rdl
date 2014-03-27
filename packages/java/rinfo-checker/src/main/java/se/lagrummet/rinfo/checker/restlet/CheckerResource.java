package se.lagrummet.rinfo.checker.restlet;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;

import org.openrdf.repository.Repository;

import org.restlet.*;
import org.restlet.data.*;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;

import se.lagrummet.rinfo.base.rdf.GritTransformer;
import se.lagrummet.rinfo.base.rdf.RDFUtil;

import se.lagrummet.rinfo.main.storage.StorageHandler;

import se.lagrummet.rinfo.checker.Checker;
import se.lagrummet.rinfo.checker.CheckerTool;


public class CheckerResource extends Resource {

    public CheckerResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_HTML));
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
            int maxEntries = !StringUtils.isEmpty(maxEntriesStr)?
                Integer.parseInt(maxEntriesStr) : -1;

            CheckerTool checkerTool =
                (CheckerTool) getContext().getAttributes().get("checkerTool");
            Checker checker = checkerTool.createChecker();
            checker.setMaxEntries(maxEntries);

            final GritTransformer logXhtmlTransformer =
                (GritTransformer) getContext().getAttributes().get("logXhtmlTransformer");
            try {
                Repository logRepo = checker.checkFeed(feedUrl);
                final InputStream ins = RDFUtil.toInputStream(logRepo, "application/rdf+xml", true);
                getResponse().setEntity(new WriterRepresentation(MediaType.TEXT_HTML) {
                    public void write(Writer writer) throws IOException {
                        try {
                            logXhtmlTransformer.writeXhtml(ins, writer);
                        } finally {
                            ins.close();
                        }
                    }
                });
            } finally {
                checker.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
