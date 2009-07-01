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

    boolean hasCollected(Entry entry) {
        // FIXME: remove registry(?) and always use:
        //return (depotEntry != null && sourceIsNotAnUpdate(sourceEntry, depotEntry))
        return registry.hasCollected(entry)
    }

    void close() {
        registry.shutdown()
    }

    void storeEntry(Feed sourceFeed, Entry sourceEntry,
            List<SourceContent> contents, List<SourceContent> enclosures) {

        URI entryId = sourceEntry.getId().toURI()
        if (hasCollected(sourceEntry)) {
            if (logger.isDebugEnabled())
                logger.debug("skipping collected entry <${entryId}> [${entry.updated}]")
            return
        }
        logger.info("Collecting entry <${entryId}>..")
        DepotEntry depotEntry = depot.getEntry(entryId)
        Date timestamp = new Date()

        try {
            boolean entryIsNew = true
            if (depotEntry == null) {
                logger.info("New entry <${entryId}>.")
                depotEntry = depot.createEntry(
                        entryId, timestamp, contents, enclosures, false)
            } else {
                entryIsNew = false
                if (sourceIsNotAnUpdate(sourceEntry, depotEntry)) {
                    logger.info("Encountered collected entry <" +
                            sourceEntry.getId()+"> at [" +
                            sourceEntry.getUpdated()+"].")
                    return
                }
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
                if (entryIsNew)
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
            logger.error("Error storing entry!", e)
            depotEntry.rollback()
            return
        }
    }

    void deleteEntry(Feed sourceFeed, URI sourceEntryId, Date deletedDate) {
        // FIXME: saveSourceMetaInfo (which may be present)
        def entryId = registry.getDepotIdBySourceId(sourceEntryId)
        // TODO: this being null means we have lost collector metadata!
        DepotEntry depotEntry = depot.getEntry(entryId)
        logger.info("Deleting entry <${entryId}>.")
        depotEntry.delete(deletedDate)
        for (StorageHandler handler : storageHandlers) {
            handler.onDelete(this, depotEntry)
        }
        registry.logUpdatedEntry(sourceFeed, sourceEntryId, deletedDate, depotEntry)
        collectedBatch.add(depotEntry)
        //TODO:..registry.logDeletedEntry
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
