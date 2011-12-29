package se.lagrummet.rinfo.checker.restlet;

import java.io.*;
import java.net.*;
import java.util.*;

import static se.lagrummet.rinfo.base.TransformerUtil.newTemplates;
import se.lagrummet.rinfo.base.rdf.GritTransformer;

import se.lagrummet.rinfo.main.storage.StorageHandler;
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

import se.lagrummet.rinfo.checker.Checker;
import se.lagrummet.rinfo.checker.CheckerTool;


public class CheckerApplication extends Application {

    private String mediaDirUrl = "war:///media/";
    private List<StorageHandler> handlers = new ArrayList<StorageHandler>();

    @Override
    public Restlet createInboundRoot() {
        try {
            getContext().getAttributes().putIfAbsent("checkerTool",
                    new CheckerTool());
            // TODO: set mediabase param
            GritTransformer logXhtmlTransformer = new GritTransformer(
                    newTemplates(CheckerApplication.class, "/xslt/checker_collector_log.xslt"));
            getContext().getAttributes().putIfAbsent("logXhtmlTransformer", logXhtmlTransformer);
            Router router = new Router(getContext());
            router.attachDefault(CheckerResource.class);
            router.attach("/media", new Directory(getContext(), mediaDirUrl));
            return router;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = (args.length > 0)? Integer.parseInt(args[0]) : 8182;
        final String mediaDir = args[1];
        CheckerApplication checkerApp = new CheckerApplication();
        checkerApp.mediaDirUrl = new File(mediaDir).toURI().toURL().toString();
        Component cmp = new Component();
        cmp.getServers().add(Protocol.HTTP, port);
        cmp.getClients().add(Protocol.FILE);
        cmp.getDefaultHost().attach("", checkerApp);
        cmp.start();
    }

}
