package se.lagrummet.rinfo.main.storage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.configuration.ConfigurationUtils

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree
import se.lagrummet.rinfo.store.depot.DepotEntry


class SourceFeedsConfigHandler implements StorageHandler {

    private final Logger logger = LoggerFactory.getLogger(SourceFeedsConfigHandler)

    FeedCollectScheduler collectScheduler

    URI configurationEntryId
    String sourceFeedsQuery

    public SourceFeedsConfigHandler(FeedCollectScheduler collectScheduler,
            URI configurationEntryId) {
        sourceFeedsQuery = ConfigurationUtils.locate("rinfo_source_feeds.rq").text
        this.collectScheduler = collectScheduler
        this.configurationEntryId = configurationEntryId
    }

    void onStartup(StorageSession storageSession) throws Exception {
        DepotEntry depotEntry = storageSession.getDepot().getEntry(
                configurationEntryId)
        if (depotEntry != null) {
            onModified(storageSession, depotEntry, false)
        }
    }

    void onModified(StorageSession storageSession, DepotEntry depotEntry,
            boolean created) throws Exception {
        if (!isConfigurationEntry(depotEntry)) {
            return
        }
        def credentials = storageSession.getCredentials()
        if (!credentials.isAdmin()) {
            throw new Exception(
                    "Not allowed to configure sources: not an admin session!")
            // TODO: throw new NotAllowedException(credentials)
        }
        def feedUrls = new ArrayList<URL>()
        Repository repo = EntryRdfReader.readRdf(depotEntry)
        // TODO:IMPROVE: configure, use raw sparql, and/or make this more failsafe.
        /* or..: * /
        walker.prefixes["dct"] = "..."
        walker.prefixes["iana"] = "..."
        walker.aboutURI("tag:lagrummet.se,2009:rinfo")
        for (Object source = walker.pushRel("dct:source"); walker.popRel();) {
            feedUrls.add(walker.rel("iana:current").asURI())
        }
        /* */
        def tree = new SparqlTree().runQuery(repo, sourceFeedsQuery)
        tree.rinfoset.each {
            it.source.each {
                feedUrls.add(new URL(it.feed['$uri']))
            }
        }
        /**/
        logger.debug("Setting public source feed urls, from " +
                configurationEntryId+", to: ${feedUrls}")
        collectScheduler.setPublicSourceFeedUrls(feedUrls)
    }

    void onDelete(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
        if (!isConfigurationEntry(depotEntry)) {
            return
        }
        collectScheduler.setPublicSourceFeedUrls(null)
    }

    boolean isConfigurationEntry(DepotEntry depotEntry) {
        return depotEntry.getId().equals(configurationEntryId)
    }

}
