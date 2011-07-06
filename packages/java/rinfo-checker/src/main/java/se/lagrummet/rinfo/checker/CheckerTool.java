package se.lagrummet.rinfo.checker;

import java.io.*;
import java.net.*;
import java.util.*;

import org.openrdf.repository.Repository;

import se.lagrummet.rinfo.base.rdf.RDFUtil;
import se.lagrummet.rinfo.main.storage.StorageHandler;
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler;

public class CheckerTool {

    static String systemBaseUri = "http://rinfo.lagrummet.se/system/";
    static String entryDatasetUri = "tag:lagrummet.se,2009:rinfo";
    static List<StorageHandler> handlers = new ArrayList<StorageHandler>();

    public static void main(String[] args) throws Exception {
        String feedUrl = args[0];
        Checker checker = new Checker(systemBaseUri, entryDatasetUri);
        checker.setMaxEntries(10);
        checker.setHandlers(handlers);
        try {
            Repository logRepo = checker.checkFeed(feedUrl);
            String mtype = "application/rdf+xml";
            RDFUtil.serialize(logRepo, mtype, System.out);
        } finally {
            checker.shutdown();
        }
    }

    static void initializeHandlers() throws Exception {
        EntryRdfValidatorHandler uriMinterHandler = new EntryRdfValidatorHandler(
                "/publ/",
                "http://rinfo.lagrummet.se/sys/uri",
                "http://rinfo.lagrummet.se/sys/uri/space#");
        handlers.add(uriMinterHandler);
        Checker adminChecker = new Checker(systemBaseUri, entryDatasetUri);
        adminChecker.setHandlers(handlers);
        // TODO: read just the URIMinter config directly <http://rinfo.lagrummet.se/sys/uri>?
        URL adminFeedUrl = new URL("http://admin.lagrummet.se/feed/current.atom");
        try {
            adminChecker.checkFeed(adminFeedUrl, true);
        } finally {
            adminChecker.shutdown();
        }
    }

}
