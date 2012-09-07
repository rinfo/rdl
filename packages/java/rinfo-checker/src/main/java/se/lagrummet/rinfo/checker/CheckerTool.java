package se.lagrummet.rinfo.checker;

import java.io.*;
import java.net.*;
import java.util.*;

import org.openrdf.repository.Repository;

import org.apache.commons.configuration.DefaultConfigurationBuilder;

import se.lagrummet.rinfo.base.rdf.RDFUtil;
import se.lagrummet.rinfo.main.Components;
import static se.lagrummet.rinfo.main.Components.ConfigKey;
import se.lagrummet.rinfo.main.storage.StorageHandler;
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler;

public class CheckerTool {

    public String reportBaseUri;
    public String entryDatasetUri;
    public String adminFeedUrl;

    List<StorageHandler> handlers = new ArrayList<StorageHandler>();

    public CheckerTool() throws Exception {
        Components components = new Components(
                new DefaultConfigurationBuilder("config.xml").getConfiguration());
        this.reportBaseUri = components.configString(ConfigKey.REPORT_BASE_URI);
        this.entryDatasetUri = components.configString(ConfigKey.SYSTEM_DATASET_URI);
        this.adminFeedUrl = components.configString(ConfigKey.ADMIN_FEED_URL);
        initializeHandlers(components);
    }

    void initializeHandlers(Components components) throws Exception {
        EntryRdfValidatorHandler rdfValidatorHandler =
            components.createEntryRdfValidatorHandler();
        handlers.add(rdfValidatorHandler);
        Checker adminChecker = createChecker();
        adminChecker.relevantEntries.addAll(rdfValidatorHandler.getVocabEntryIds());
        adminChecker.relevantEntries.add(rdfValidatorHandler.getValidationEntryId());
        adminChecker.relevantEntries.add(rdfValidatorHandler.getUriSpaceEntryId());
        try {
            adminChecker.checkFeed(new URL(adminFeedUrl), true);
        } finally {
            adminChecker.shutdown();
        }
    }

    public Checker createChecker() throws Exception {
        Checker checker = new Checker(reportBaseUri, entryDatasetUri);
        checker.setHandlers(handlers);
        return checker;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: CMD <feedUrl> |maxEntries]");
            System.exit(0);
        }
        String feedUrl = args[0];

        Checker checker = new CheckerTool().createChecker();
        checker.setMaxEntries((args.length > 1)? Integer.parseInt(args[1]) : 10);
        try {
            Repository logRepo = checker.checkFeed(feedUrl);
            String mtype = "application/rdf+xml";
            RDFUtil.serialize(logRepo, mtype, System.out, true);
        } finally {
            checker.shutdown();
        }
    }

}
