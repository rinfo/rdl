package se.lagrummet.rinfo.main.storage

import org.apache.commons.configuration.ConfigurationUtils

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree
import se.lagrummet.rinfo.store.depot.DepotEntry


class SourceFeedsConfigHandler extends AbstractStorageHandler {

    FeedCollectScheduler collectScheduler

    URI configurationEntryId
    String sourceFeedsQuery

    public SourceFeedsConfigHandler(FeedCollectScheduler collectScheduler,
            URI configurationEntryId) {
        sourceFeedsQuery = ConfigurationUtils.locate("rinfo_source_feeds.rq").text
        this.collectScheduler = collectScheduler
        this.configurationEntryId = configurationEntryId
    }

    boolean isConfigurationEntry(DepotEntry depotEntry) {
        return depotEntry.getId().equals(configurationEntryId)
    }

    void onStartup(StorageSession storageSession) throws Exception {
        DepotEntry depotEntry = storageSession.getDepot().getEntry(
                configurationEntryId)
        if (depotEntry != null) {
            onEntry(storageSession, depotEntry, false)
        }
    }

    void onEntry(StorageSession storageSession, DepotEntry depotEntry,
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
        def tree = SparqlTree.runQuery(repo, sourceFeedsQuery)
        tree.rinfospace[0].source.each {
            feedUrls << new URL(it.feed['$uri'])
        }
        collectScheduler.setPublicSourceFeedUrls(feedUrls)
    }

    void onDelete(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
        if (!isConfigurationEntry(depotEntry)) {
            return
        }
        collectScheduler.setSourceFeedUrls(null)
    }

}
