package se.lagrummet.rinfo.store.depot;

import java.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;
import org.apache.abdera.i18n.iri.IRI;


public class AtomIndexer {

    private Atomizer atomizer;
    private FileDepotBackend backend;

    private DepotEntryBatch entryBatch;
    //private Feed currentFeed;
    //private Date currentDate;
    //private int batchCount = 0;
    //private Feed youngestArchFeed;

    private final Logger logger = LoggerFactory.getLogger(AtomIndexer.class);


    public AtomIndexer(Atomizer atomizer, FileDepotBackend backend) {
        this.atomizer = atomizer;
        this.backend = backend;
    }

    public void close() throws DepotWriteException {
        if (entryBatch == null)
            return;
        try {
            indexEntries(entryBatch);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
        entryBatch = null;
    }

    protected void indexEntry(DepotEntry entry) throws DepotWriteException {
        if (entryBatch == null) {
            entryBatch = new DepotEntryBatch((FileDepot) entry.getDepot());
        }
        // TODO: use a stable indexing, e.g. addOrdered which incrementally
        // writes to text index (instead of last-call indexEntries above.)
        entryBatch.add(entry);
        try {
            Entry atomEntry = atomizer.createAtomEntry(entry);
            writeAtomEntry((FileDepotEntry) entry, atomEntry);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }


    protected void indexEntries(DepotEntryBatch entryBatch)
            throws DepotWriteException, IOException {

        String subscriptionPath = atomizer.getSubscriptionPath();

        Feed currentFeed = getFeed(subscriptionPath);
        if (currentFeed == null) {
            currentFeed = atomizer.newFeed(subscriptionPath);
        }

        Feed youngestArchFeed = getPrevArchiveAsFeed(currentFeed);

        int batchCount = currentFeed.getEntries().size();

        Date currentDate = null;
        if (batchCount > 0) {
            currentDate = currentFeed.getEntries().get(0).getUpdated();
        }

        for (DepotEntry depotEntry : entryBatch) {
            batchCount++;

            Date nextDate = depotEntry.getUpdated();
            if (currentDate != null) {
                if (nextDate.compareTo(currentDate) < 0) {
                    throw new DepotIndexException(
                            "New entry to index must be younger than previous." +
                            " Entry with id <"+depotEntry.getId()+"> was updated at ["
                            +nextDate+"], previous entry at ["+currentDate+"].");
                }
            }
            currentDate = nextDate;

            if (batchCount > atomizer.getFeedBatchSize()) { // save as archive
                atomizer.setArchive(currentFeed, true);
                // path based on date of youngest entry..
                String archPath = atomizer.pathToArchiveFeed(depotEntry.getUpdated());
                currentFeed.getSelfLink().setHref(archPath);
                atomizer.setCurrentFeedHref(currentFeed, subscriptionPath);
                if (youngestArchFeed != null) {
                    atomizer.setNextArchive(youngestArchFeed,
                            atomizer.uriPathFromFeed(currentFeed));
                    writeFeed(youngestArchFeed); // re-write..
                }
                writeFeed(currentFeed);
                youngestArchFeed = currentFeed;
                currentFeed = atomizer.newFeed(subscriptionPath);
                batchCount = 1; // current item ends up in the new feed
            }

            if (youngestArchFeed != null) {
                atomizer.setPreviousArchive(currentFeed,
                        atomizer.uriPathFromFeed(youngestArchFeed));
            }

            logger.info("Indexing entry: <"+depotEntry.getId()+"> ["+depotEntry.getUpdated()+"]");

            atomizer.addEntryToFeed(depotEntry, currentFeed);
            /* TODO:IMPROVE:
                Dry out, unless generating new (when we know all, incl. deleteds..)
                If so, historical entries must know if their current is deleted!
            dryOutHistoricalEntries(depotEntry)
            */

        }
        writeFeed(currentFeed); // as subscription feed
    }

    protected Feed getFeed(String uriPath) throws IOException {
        File feedFile = backend.getFeedFile(uriPath);
        Feed feed = null;
        try {
            InputStream inStream = new FileInputStream(feedFile);
            try {
                feed = (Feed) Abdera.getInstance().getParser().parse(
                        inStream).getRoot();
                feed.complete();
            } finally {
                inStream.close();
            }
        } catch (FileNotFoundException e) {
            return null;
        }
        return feed;
    }

    protected Feed getPrevArchiveAsFeed(Feed feed) throws IOException {
        IRI prev = atomizer.getPreviousArchive(feed);
        if (prev == null) {
            return null;
        }
        return getFeed(prev.toString());
    }

    /* TODO: to use for e.g. "emptying" ("drying out") deleted entries.
        Search in feed folder by date, time; opt. offset (if many of same in
        same instant?).
    protected Feed findFeedForDateTime(Date date) {
        .. findFeedForDateTime(pathToArchiveFeed(date))
        return null;
    }
    */

    protected void writeFeed(Feed feed) throws DepotWriteException {
        String uriPath = atomizer.uriPathFromFeed(feed);
        logger.info("Writing feed: <"+uriPath+">");
        try {
            File feedFile = backend.getFeedFile(uriPath);
            File feedDir = feedFile.getParentFile();
            if (!feedDir.exists()) {
                FileUtils.forceMkdir(feedDir);
            }
            OutputStream outStream = new FileOutputStream(feedFile);
            try {
                if (atomizer.getPrettyXml()) {
                    feed.writeTo("prettyxml", outStream);
                } else {
                    feed.writeTo(outStream);
                }
            } finally {
                outStream.close();
            }
        } catch (Exception e) {
            throw new DepotWriteException(e);
        }
    }

    protected void writeAtomEntry(FileDepotEntry depotEntry, Entry atomEntry)
            throws DepotWriteException {
        try {
            File entryFile = depotEntry.newContentFile(atomizer.ATOM_ENTRY_MEDIA_TYPE);
            OutputStream outStream = new FileOutputStream(entryFile);
            if (atomizer.getPrettyXml()) {
                atomEntry.writeTo("prettyxml", outStream);
            } else {
                atomEntry.writeTo(outStream);
            }
            outStream.close();
        } catch (Exception e) {
            throw new DepotWriteException(e);
        }
    }

}
