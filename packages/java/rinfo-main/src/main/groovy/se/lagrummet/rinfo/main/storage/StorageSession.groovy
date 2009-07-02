package se.lagrummet.rinfo.main.storage


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.repository.Repository

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Link
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.SourceContent
import se.lagrummet.rinfo.store.depot.DepotEntryBatch
import se.lagrummet.rinfo.store.depot.DuplicateDepotEntryException


class StorageSession {

    private final Logger logger = LoggerFactory.getLogger(StorageSession)

    public static final String SOURCE_META_FILE_NAME = "collector-source-info.entry"

    Depot depot
    Collection<StorageHandler> storageHandlers =
            new ArrayList<StorageHandler>()
    FeedCollectorRegistry registry

    private DepotEntryBatch collectedBatch

    StorageSession(Depot depot,
            Collection<StorageHandler> storageHandlers,
            FeedCollectorRegistry registry) {
        this.depot = depot
        this.storageHandlers = storageHandlers
        this.registry = registry
    }

    void beginPage(URL pageUrl, Feed feed) {
        registry.logVisitedFeedPage(pageUrl, feed)
        collectedBatch = depot.makeEntryBatch()
    }

    void endPage() {
        depot.indexEntries(collectedBatch)
        collectedBatch = null
    }

    boolean hasCollected(Entry sourceEntry) {
        // TODO:? remove registry(?) or reuse eventRegistry with:
        //return registry.hasCollected(sourceEntry)
        return hasCollected(sourceEntry, depot.getEntry(sourceEntry.getId().toURI()))
    }

    boolean hasCollected(Entry sourceEntry, DepotEntry depotEntry) {
        return (depotEntry != null && sourceIsNotAnUpdate(sourceEntry, depotEntry))
    }

    void close() {
        registry.shutdown()
    }

    void storeEntry(Feed sourceFeed, Entry sourceEntry,
            List<SourceContent> contents, List<SourceContent> enclosures) {

        URI entryId = sourceEntry.getId().toURI()
        logger.info("Collecting entry <${entryId}>..")
        DepotEntry depotEntry = depot.getEntry(entryId)

        // TODO: Not needed? storeEntry should not be called if hasCollected is false
        // (via stopOnEntry)?
        if (hasCollected(sourceEntry, depotEntry)) {
            logger.info("Encountered collected entry with id=<" +
                    sourceEntry.getId()+">, updated=[" +
                    sourceEntry.getUpdated()+"]. Skipping.")
            return
        }

        boolean createEntry = (depotEntry == null)
        Date timestamp = new Date()
        try {
            if (createEntry) {
                logger.info("New entry <${entryId}>.")
                depotEntry = depot.createEntry(
                        entryId, timestamp, contents, enclosures, false)
            } else {
                // NOTE: If source has been collected but appears as newly published:
                if (!(sourceEntry.getUpdated() > sourceEntry.getPublished())) {
                    logger.error("Collected entry <"+sourceEntry.getId() +
                            " exists as <"+entryId +
                            "> but does not appear as updated:" +
                            sourceEntry)
                    throw new DuplicateDepotEntryException(depotEntry);
                }
                logger.info("Updating entry <${entryId}>.")
                depotEntry.lock()
                depotEntry.update(timestamp, contents, enclosures)
            }
            saveSourceMetaInfo(sourceFeed, sourceEntry, depotEntry)

            for (StorageHandler handler : storageHandlers) {
                if (createEntry)
                    handler.onCreate(this, depotEntry)
                else
                    handler.onUpdate(this, depotEntry)
            }

            collectedBatch.add(depotEntry)
            depotEntry.unlock()
            registry.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)

        } catch (Exception e) {
            /* FIXME: handle errors:
                - retriable:
                    java.net.SocketException

                - source errors (report and log):
                    javax.net.ssl.SSLPeerUnverifiedException
                    MissingRdfContentException
                    URIComputationException
                    DuplicateDepotEntryException
                    SourceCheckException (from SourceContent#writeTo), ...

               Index the ok ones, *rollback* last depotEntry and *report error*!
            */
            logger.error("Error storing entry:", e)
            depotEntry.rollback()
            registry.logError(e, timestamp, sourceFeed, sourceEntry)
            return
        }
    }

    void deleteEntry(Feed sourceFeed, URI entryId, Date deletedDate) {
        DepotEntry depotEntry = depot.getEntry(entryId)
        if (depot == null) {
            // TODO: means we have lost collector metadata?
        }
        logger.info("Deleting entry <${entryId}>.")
        // TODO:? saveDeletionMetaInfo? (smart deletion detect; e.g. feed + tombstone)
        // .. or is tombstone in feed enough (for e.g. event index)?
        depotEntry.delete(deletedDate)
        for (StorageHandler handler : storageHandlers) {
            handler.onDelete(this, depotEntry)
        }
        registry.logDeletedEntry(sourceFeed, entryId, deletedDate, depotEntry)
        collectedBatch.add(depotEntry)
    }

    protected static boolean sourceIsNotAnUpdate(Entry sourceEntry,
            DepotEntry depotEntry) {
        File savedSourceEntryFile = depotEntry.getMetaFile(SOURCE_META_FILE_NAME)
        Entry savedSourceEntry = null
        try {
            savedSourceEntry = (Entry) Abdera.getInstance().getParser().parse(
                    new FileInputStream(savedSourceEntryFile)).getRoot();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Entry <"+depotEntry.getId() +
                    "> is missing expected meta file <"+savedSourceEntryFile+">.")
        }
        // TODO:IMPROVE:?
        // If depotEntry; check stored source and allow update if *both*
        //     sourceEntry.updated>.created (above) *and* > depotEntry.updated..
        //     .. and "source feed" is "same as last"? (indirected via rdf facts)?
        // TODO:? Assert id == id (& feed.id == ..)?
        return !(sourceEntry.getUpdated() > savedSourceEntry.getUpdated())
    }

    protected static void saveSourceMetaInfo(Feed sourceFeed, Entry sourceEntry,
            DepotEntry depotEntry) {
        File savedSourceEntryFile = depotEntry.getMetaFile(SOURCE_META_FILE_NAME)
        Entry sourceEntryClone = sourceEntry.clone()
        // TODO:IMPROVE: remove tombstones..
        sourceEntryClone.setSource(sourceFeed)
        // TODO:IMPROVE: is this way of setting base URI enough?
        sourceEntryClone.setBaseUri(sourceEntryClone.getSource().getResolvedBaseUri())
        sourceEntryClone.getSource().setBaseUri(null)
        sourceEntryClone.writeTo(new FileOutputStream(savedSourceEntryFile))
    }

}
