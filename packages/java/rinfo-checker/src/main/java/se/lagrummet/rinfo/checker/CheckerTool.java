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

    public static void main(String[] args) throws Exception {
        List<StorageHandler> handlers = new ArrayList<StorageHandler>();
        String feedUrl = args[0];
        URL adminFeedUrl = new URL("http://admin.lagrummet.se/feed/current.atom");
        Checker checker = new Checker(systemBaseUri, entryDatasetUri);
        checker.setMaxEntries(10);
        initializeHandlers(adminFeedUrl, handlers);
        checker.setHandlers(handlers);
        try {
            Repository logRepo = checker.checkFeed(feedUrl);
            String mtype = "application/rdf+xml";
            RDFUtil.serialize(logRepo, mtype, System.out, true);
        } finally {
            checker.shutdown();
        }
    }

    // TODO: don't hardcode this config (get from main?)
    public static void initializeHandlers(URL adminFeedUrl,
            List<StorageHandler> handlers) throws Exception {
        List<String> vocabEntryIds = new ArrayList<String>();
        vocabEntryIds.add("http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ");
        vocabEntryIds.add("http://rinfo.lagrummet.se/ext/models");
        EntryRdfValidatorHandler rdfValidatorHandler = new EntryRdfValidatorHandler(
                "/publ/",
                vocabEntryIds,
                "http://rinfo.lagrummet.se/sys/uri",
                "http://rinfo.lagrummet.se/sys/uri/space#");
        handlers.add(rdfValidatorHandler);
        Checker adminChecker = new Checker(systemBaseUri, entryDatasetUri);
        adminChecker.setHandlers(handlers);
        // TODO: read just the URIMinter config directly <http://rinfo.lagrummet.se/sys/uri>?
        try {
            adminChecker.checkFeed(adminFeedUrl, true);
        } finally {
            adminChecker.shutdown();
        }
    }

}
