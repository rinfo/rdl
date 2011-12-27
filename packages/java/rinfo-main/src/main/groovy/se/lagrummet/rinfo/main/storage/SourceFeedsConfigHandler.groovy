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
    URI systemDatasetUri

    public SourceFeedsConfigHandler(FeedCollectScheduler collectScheduler,
            URI configurationEntryId,
            URI systemDatasetUri) {
        this.collectScheduler = collectScheduler
        this.configurationEntryId = configurationEntryId
        this.systemDatasetUri = systemDatasetUri
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
            throw new NotAllowedException(
                    "Not allowed to configure sources: not an admin source!", credentials)
        }
        def sources = new ArrayList<CollectorSource>()
        Repository repo = EntryRdfReader.readRdf(depotEntry)
        def conn = repo.getConnection()
        try {
            def desc = new Describer(conn).
                    setPrefix("dct", "http://purl.org/dc/terms/").
                    setPrefix("iana", "http://www.iana.org/assignments/relation/")
            def dataset = desc.findDescription(systemDatasetUri.toString())
            if (dataset == null) {
                logger.warn("Found no sources for <"+ systemDatasetUri +"> in "+
                        configurationEntryId)
                return
            }
            for (source in dataset.getRels("dct:source")) {
                sources.add(new CollectorSource(
                            new URI(source.getString("dct:identifier")),
                            new URL(source.getObjectUri("iana:current"))))
            }
        } finally {
            conn.close()
        }
        logger.debug("Setting public sources , from " +
                configurationEntryId + ", to: ${sources}")
        collectScheduler.setSources(sources)
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
