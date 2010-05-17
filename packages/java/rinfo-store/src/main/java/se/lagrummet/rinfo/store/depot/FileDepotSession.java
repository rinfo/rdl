package se.lagrummet.rinfo.store.depot;

import java.io.IOException;
import java.io.File;
import java.util.Iterator;
import java.util.Date;
import java.util.List;
import java.net.URI;

import org.apache.commons.io.FileUtils;


public class FileDepotSession implements DepotSession {

    private FileDepot depot;
    private DepotEntryBatch batch;
    private DepotEntry pending;

    public FileDepotSession(FileDepot depot) {
        this.depot = depot;
    }

    public Depot getDepot() { return depot; }

    public DepotEntry getEntry(URI entryUri) throws DepotReadException {
        return depot.getEntry(entryUri);
    }

    public boolean hasEntry(URI entryUri) {
        return depot.hasEntry(entryUri);
    }

    // NOTE: Writing initializes state; *must* call close()

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents)
            throws DepotReadException, DepotWriteException {
        return createEntry(entryUri, created, contents, null);
    }

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures)
            throws DepotReadException, DepotWriteException {
        commitPending();
        depot.assertWithinBaseUri(entryUri);
        String uriPath = entryUri.getPath();
        DepotEntry entry = depot.backend.newBlankEntry(uriPath);
        pending = entry;
        entry.create(created, contents, enclosures, false);
        onEntryModified(entry);
        return entry;
    }

    public void update(DepotEntry entry, Date updated,
            List<SourceContent> contents)
            throws DepotReadException, DepotWriteException {
        update(entry, updated, contents, null);
    }

    public void update(DepotEntry entry, Date updated,
            List<SourceContent> contents,
            List<SourceContent> enclosures)
            throws DepotReadException, DepotWriteException {
        commitPending();
        pending = entry;
        entry.lock();
        entry.update(updated, contents, enclosures);
        onEntryModified(entry);
    }

    public void delete(DepotEntry entry, Date deleted)
            throws DeletedDepotEntryException,
                   DepotReadException, DepotWriteException {
        commitPending();
        pending = entry;
        entry.lock();
        entry.delete(deleted);
        onEntryModified(entry);
    }


    public void generateIndex() throws DepotWriteException {
        // TODO: move to backend
        File feedDir = new File(
                depot.baseDir, depot.atomizer.getFeedPath());
        if (!feedDir.exists()) {
            if (!feedDir.mkdir()) {
                throw new DepotWriteException(
                        "Cannot create entry content directory: " + feedDir);
            }
        }
        DepotEntryBatch entryBatch = makeEntryBatch();

        for (Iterator<DepotEntry> iter = depot.backend.iterateEntries(
                    depot.atomizer.getIncludeHistorical(),
                    depot.atomizer.getIncludeDeleted() );
                iter.hasNext(); ) {
            entryBatch.add(iter.next());
        }
        try {
            FileUtils.cleanDirectory(feedDir);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
        indexEntries(entryBatch);
    }

    void indexEntries(DepotEntryBatch entryBatch)
            throws DepotWriteException {
        try {
            depot.atomizer.indexEntries(entryBatch);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }

    DepotEntryBatch makeEntryBatch() {
        return new DepotEntryBatch(depot);
    }


    //== Session state management for index-on-write ==

    public void close() throws DepotWriteException {
        commitPending();
        if (batch != null) {
            indexEntries(batch);
            batch = null;
        }
    }

    protected void commitPending() throws DepotWriteException {
        if (pending != null) {
            if (batch == null) {
                batch = makeEntryBatch();
            }
            // TODO: use a stable indexing, e.g. addOrdered which incrementally
            // writes to text index (instead of last-call indexEntries above.)
            batch.add(pending);
            pending.unlock();
        }
    }

    public void rollbackPending() throws DepotWriteException {
        if (pending == null) {
            throw new IllegalStateException(
                    "Cannot rollback: nothing is pending.");
        }
        pending.rollback();
        pending = null;
    }

    protected void onEntryModified(DepotEntry entry)
            throws DepotWriteException {
        try {
            depot.atomizer.generateAtomEntryContent(entry);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
        // TODO:? update latest feed index file (may create new file and modify
        // next-to-last (add next-archive)?)! Since any modifying means
        // a new updated depotEntry in the feeds..
    }

}
