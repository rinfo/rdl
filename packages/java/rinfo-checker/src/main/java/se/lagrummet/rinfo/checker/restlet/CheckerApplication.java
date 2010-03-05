package se.lagrummet.rinfo.checker.restlet;

import java.io.*;
import java.net.*;
import java.util.*;

import se.lagrummet.rinfo.base.URIMinter;

import se.lagrummet.rinfo.main.storage.StorageHandler;
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler;

import org.restlet.*;
import org.restlet.data.Protocol;

import se.lagrummet.rinfo.checker.Checker;


/* TODO:

Goals:
 - based on collector code in rinfo-main
 - services:
   - check feed by url: run (partial?) collect (one page: follow no archive?)
   - check rdf by url: run handlers.. or feed-uri + "just entry X"?
   - check feed *source* (body)): attrs per entry (not in collector? add-hoc "validate"?)

Suggested changes in rinfo-main,-depot,-collector:

 - make CollectorLog an interface? At least configurable..
   - this checker log needs both collected+collect+errors
   - "normal" skips collected (stored in main feed)
     .. in that mode, we'd need to combine depot feed (as collect success log) + checkLog

 - StorageSession (subclass?):
   - dummyDepot(Backend)
     - in-mem *Backend*? (if so, depotEntry.getMetaFile -> getMetaInputStream/-Out..?)
     - or no depot? write to "/dev/null" to make datachecks? pdf:s? check some?
  - def checkLog = new CheckDataCollectorLog

*/
public class CheckerApplication extends Application {

    private String mediaDirUrl = "war:///media/";
    private List<StorageHandler> handlers = new ArrayList<StorageHandler>();

    @Override
    public Restlet createRoot() {
        try {
            initializeHandlers();
            getContext().getAttributes().putIfAbsent("handlers", handlers);
            Router router = new Router(getContext());
            router.attachDefault(CheckerResource.class);
            router.attach("/media", new Directory(getContext(), mediaDirUrl));
            return router;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void initializeHandlers() throws Exception {
        EntryRdfValidatorHandler uriMinterHandler = new EntryRdfValidatorHandler(
                new URI("http://rinfo.lagrummet.se/sys/uri"), "/publ/");
        handlers.add(uriMinterHandler);
        Checker adminChecker = new Checker();
        adminChecker.setHandlers(handlers);
        // TODO: read just the URIMinter config directly <http://rinfo.lagrummet.se/sys/uri>?
        URL adminFeedUrl = new URL("http://admin.lagrummet.se/feed/current.atom");
        try {
            adminChecker.checkFeed(adminFeedUrl, true);
        } finally {
            adminChecker.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = (args.length > 0)? Integer.parseInt(args[0]) : 8182;
        final String mediaDir = args[1];
        CheckerApplication checkerApp = new CheckerApplication();
        checkerApp.mediaDirUrl = new File(mediaDir).toURL().toString();
        Component cmp = new Component();
        cmp.getServers().add(Protocol.HTTP, port);
        cmp.getClients().add(Protocol.FILE);
        cmp.getDefaultHost().attach("", checkerApp);
        cmp.start();
    }

}
