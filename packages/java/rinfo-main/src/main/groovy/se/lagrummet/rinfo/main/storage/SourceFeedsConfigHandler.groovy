package se.lagrummet.rinfo.main.storage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.store.depot.DepotEntry


class SourceFeedsConfigHandler implements StorageHandler {

    private final Logger logger = LoggerFactory.getLogger(SourceFeedsConfigHandler)

    FeedCollectScheduler collectScheduler

    URI configurationEntryId

    public SourceFeedsConfigHandler(FeedCollectScheduler collectScheduler,
            URI configurationEntryId) {
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
        def conn = repo.getConnection()
        try {
            def desc = new Describer(conn).
                    setPrefix("dct", "http://purl.org/dc/terms/").
                    setPrefix("iana", "http://www.iana.org/assignments/relation/")
            def dataset = desc.findDescription("tag:lagrummet.se,2009:rinfo")
            for (source in dataset.getRels("dct:source")) {
                feedUrls.add(new URL(source.getObjectUri("iana:current")))
            }
        } finally {
            conn.close()
        }
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
