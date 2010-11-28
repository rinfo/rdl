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
import se.lagrummet.rinfo.store.depot.DepotSession
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.SourceContent
import se.lagrummet.rinfo.store.depot.DuplicateDepotEntryException

import se.lagrummet.rinfo.collector.atom.CompleteFeedEntryIdIndex;


class StorageSession {

    private final Logger logger = LoggerFactory.getLogger(StorageSession)

    public static final String VIA_META_RESOURCE = "collector-via.entry"

    StorageCredentials credentials
    Depot depot
    Collection<StorageHandler> storageHandlers =
            new ArrayList<StorageHandler>()
    DepotSession depotSession
    CollectorLogSession logSession
    CompleteFeedEntryIdIndex completeFeedEntryIdIndex

    StorageSession(StorageCredentials credentials,
            Depot depot,
            Collection<StorageHandler> storageHandlers,
            CollectorLogSession logSession,
            CompleteFeedEntryIdIndex completeFeedEntryIdIndex) {
        this.credentials = credentials
        this.depot = depot
        this.storageHandlers = storageHandlers
        this.depotSession = depot.openSession()
        this.logSession = logSession
        this.completeFeedEntryIdIndex = completeFeedEntryIdIndex
    }

    void close() {
        try {
            depotSession.close()
        } finally {
            logSession.close()
        }
    }

    void beginPage(URL pageUrl, Feed feed) {
        logSession.logFeedPageVisit(pageUrl, feed)
    }

    void endPage(URL pageUrl) {
    }

    boolean hasCollected(Entry sourceEntry) {
        return hasCollected(sourceEntry,
                depot.getEntryOrDeletedEntry(sourceEntry.getId().toURI()))
    }

    boolean hasCollected(Entry sourceEntry, DepotEntry depotEntry) {
        return (depotEntry != null && !depotEntry.isDeleted() &&
                sourceIsNotAnUpdate(sourceEntry, depotEntry))
    }

    boolean storeEntry(Feed sourceFeed, Entry sourceEntry,
            List<SourceContent> contents, List<SourceContent> enclosures) {

        URI entryId = sourceEntry.getId().toURI()
        logger.info("Examining entry <${entryId}>..")
        DepotEntry depotEntry = depot.getEntryOrDeletedEntry(entryId)

        // NOTE: Needed since even if hasCollected is true (via stopOnEntry),
        // there may be several entries with the same timestamp.
        // TODO:IMPROVE? Will this "thrash" on many true:s?
        if (hasCollected(sourceEntry, depotEntry)) {
            logger.info("Encountered collected entry with id=<" +
                    sourceEntry.getId()+">, updated=[" +
                    sourceEntry.getUpdated()+"]. Skipping.")
            return true
        }

        boolean doCreate = (depotEntry == null || depotEntry.isDeleted())
        if (depotEntry != null && depotEntry.isDeleted()) {
            depotEntry.resurrect()
        }

        Date timestamp = new Date()
        try {
            if (doCreate) {
                logger.info("New entry <${entryId}>.")
                depotEntry = depotSession.createEntry(
                        entryId, timestamp, contents, enclosures)
            } else {
                // NOTE: If source has been collected but appears as newly published:
                if (!(sourceEntry.getUpdated() > sourceEntry.getPublished())) {
                    logger.error("Collected entry <"+sourceEntry.getId() +
                            "> exists as <"+entryId +
                            "> but does not appear as updated. Source:" +
                            sourceEntry)
                    // FIXME: remove or enable:
                    //throw new DuplicateDepotEntryException(depotEntry);
                }
                logger.info("Updating entry <${entryId}>.")
                depotSession.update(depotEntry, timestamp, contents, enclosures)
            }
            setViaEntry(depotEntry, sourceFeed, sourceEntry)

            for (StorageHandler handler : storageHandlers) {
                handler.onModified(this, depotEntry, doCreate)
            }

            logSession.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)
            return true

        } catch (Exception e) {
            /* TODO: explicit handling (and logging) of different errors:
                - retriable:
                    java.net.SocketException

                - source errors (report and log):
                    javax.net.ssl.SSLPeerUnverifiedException
                    MissingRdfContentException
                    IdentifyerMismatchException
                    DuplicateDepotEntryException
                    SourceCheckException (from SourceContent#writeTo), ...

               Index the ok ones, *rollback* last depotEntry and *report error*!
            */
            logger.error("Error storing entry:", e)
            depotSession.rollbackPending()
            logSession.logError(e, timestamp, sourceFeed, sourceEntry)
            return false
        }
    }

    void deleteEntry(Feed sourceFeed, URI entryId, Date sourceDeletedDate) {
        DepotEntry depotEntry = depot.getEntryOrDeletedEntry(entryId)
        // TODO:? could this mean we have an error causing loss of collector metadata?
        if (depotEntry == null) {
            logger.warn("Could not delete entry, missing <${entryId}>.")
            return
        }
        if (depotEntry.isDeleted()) {
            logger.warn("Not deleting already deleted entry <${entryId}>.")
            return
        }
        logger.info("Deleting entry <${entryId}>.")
        // TODO:? saveDeletionMetaInfo? (smart deletion detect; e.g. feed + tombstone)
        // .. or is tombstone in feed enough (for e.g. event index)?
        // TODO: previously used sourceDeletedDate; must surely have been wrong?
        Date deletedDate = new Date()
        depotSession.delete(depotEntry, deletedDate)
        for (StorageHandler handler : storageHandlers) {
            handler.onDelete(this, depotEntry)
        }
        logSession.logDeletedEntry(
                sourceFeed, entryId, sourceDeletedDate, depotEntry)
    }

    static Entry getViaEntry(DepotEntry depotEntry) {
        InputStream viaEntryInStream = depotEntry.getMetaInputStream(VIA_META_RESOURCE)
        if (viaEntryInStream == null)
            return null
        Entry viaEntry = null
        try {
            viaEntry = (Entry) Abdera.getInstance().getParser().parse(
                    viaEntryInStream).getRoot();
        } finally {
            viaEntryInStream.close()
        }
        return viaEntry
    }

    static void setViaEntry(DepotEntry depotEntry,
            Feed sourceFeed, Entry sourceEntry) {
        Entry viaEntry = sourceEntry.clone()
        // TODO:IMPROVE: remove tombstones; except del.id == depotEntry.id if deleted..
        // TODO: fail on missing sourceFeed.id..
        viaEntry.setSource(sourceFeed)
        // TODO:IMPROVE: is this way of setting base URI enough?
        viaEntry.setBaseUri(viaEntry.getSource().getResolvedBaseUri())
        viaEntry.getSource().setBaseUri(null)
        OutputStream viaEntryOutStream = depotEntry.getMetaOutputStream(VIA_META_RESOURCE)
        try {
            viaEntry.writeTo(viaEntryOutStream)
        } finally {
            viaEntryOutStream.close()
        }
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
