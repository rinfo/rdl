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

    public static final String VIA_META_FILE_NAME = "collector-via.entry"

    StorageCredentials credentials
    Depot depot
    Collection<StorageHandler> storageHandlers =
            new ArrayList<StorageHandler>()
    CollectorLogSession logSession

    private DepotEntryBatch collectedBatch

    StorageSession(StorageCredentials credentials,
            Depot depot,
            Collection<StorageHandler> storageHandlers,
            CollectorLogSession logSession) {
        this.credentials = credentials
        this.depot = depot
        this.storageHandlers = storageHandlers
        this.logSession = logSession
    }

    void close() {
        logSession.close()
    }

    void beginPage(URL pageUrl, Feed feed) {
        logSession.logFeedPageVisit(pageUrl, feed)
        collectedBatch = depot.makeEntryBatch()
    }

    void endPage() {
        depot.indexEntries(collectedBatch)
        collectedBatch = null
    }

    boolean hasCollected(Entry sourceEntry) {
        // TODO:IMPROVE? optimize by using eventRegistry with:
        //return logSession.hasCollected(sourceEntry)
        return hasCollected(sourceEntry, depot.getEntry(sourceEntry.getId().toURI()))
    }

    boolean hasCollected(Entry sourceEntry, DepotEntry depotEntry) {
        return (depotEntry != null && sourceIsNotAnUpdate(sourceEntry, depotEntry))
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
            setViaEntry(depotEntry, sourceFeed, sourceEntry)

            for (StorageHandler handler : storageHandlers) {
                if (createEntry)
                    handler.onCreate(this, depotEntry)
                else
                    handler.onUpdate(this, depotEntry)
            }

            collectedBatch.add(depotEntry)
            depotEntry.unlock()
            logSession.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)

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
            logSession.logError(e, timestamp, sourceFeed, sourceEntry)
            return
        }
    }

    void deleteEntry(Feed sourceFeed, URI entryId, Date sourceDeletedDate) {
        DepotEntry depotEntry = depot.getEntry(entryId)
        if (depotEntry == null) {
            // TODO: means we have lost collector metadata?
            logger.warn("Could not delete entry, missing <${entryId}>.")
        }
        logger.info("Deleting entry <${entryId}>.")
        // TODO:? saveDeletionMetaInfo? (smart deletion detect; e.g. feed + tombstone)
        // .. or is tombstone in feed enough (for e.g. event index)?
        // TODO: previously used sourceDeletedDate; must surely have been wrong?
        Date deletedDate = new Date()
        depotEntry.delete(deletedDate)
        for (StorageHandler handler : storageHandlers) {
            handler.onDelete(this, depotEntry)
        }
        logSession.logDeletedEntry(
                sourceFeed, entryId, sourceDeletedDate, depotEntry)
        collectedBatch.add(depotEntry)
    }

    static Entry getViaEntry(DepotEntry depotEntry) {
        File viaEntryFile = depotEntry.getMetaFile(VIA_META_FILE_NAME)
        Entry viaEntry = null
        try {
            viaEntry = (Entry) Abdera.getInstance().getParser().parse(
                    new FileInputStream(viaEntryFile)).getRoot();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Entry <"+depotEntry.getId() +
                    "> is missing expected meta file <"+viaEntryFile+">.")
        }
        return viaEntry
    }

    static void setViaEntry(DepotEntry depotEntry,
            Feed sourceFeed, Entry sourceEntry) {
        File viaEntryFile = depotEntry.getMetaFile(VIA_META_FILE_NAME)
        Entry viaEntry = sourceEntry.clone()
        // TODO:IMPROVE: remove tombstones; except del.id == depotEntry.id if deleted..
        // TODO: fail on missing sourceFeed.id..
        viaEntry.setSource(sourceFeed)
        // TODO:IMPROVE: is this way of setting base URI enough?
        viaEntry.setBaseUri(viaEntry.getSource().getResolvedBaseUri())
        viaEntry.getSource().setBaseUri(null)
        viaEntry.writeTo(new FileOutputStream(viaEntryFile))
    }


    protected static boolean sourceIsNotAnUpdate(Entry sourceEntry,
            DepotEntry depotEntry) {
        Entry viaEntry = getViaEntry(depotEntry)
        // TODO:IMPROVE:?
        // If depotEntry; check stored source and allow update if *both*
        //     sourceEntry.updated>.created (above) *and* > depotEntry.updated..
        //     .. and "source feed" is "same as last"? (indirected via rdf facts)?
        // TODO:? Assert id == id (& feed.id == ..)?
        return !(sourceEntry.getUpdated() > viaEntry.getUpdated())
    }

}
