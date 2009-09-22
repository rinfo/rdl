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
        DepotEntry entry = depot.backend.createEntry(
                uriPath, created, contents, enclosures, false);
        onEntryModified(entry);
        pending = entry;
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
        entry.lock();
        entry.update(updated, contents, enclosures);
        onEntryModified(entry);
        pending = entry;
    }

    public void delete(DepotEntry entry, Date deleted)
            throws DeletedDepotEntryException,
                   DepotReadException, DepotWriteException {
        commitPending();
        entry.lock();
        entry.delete(deleted);
        onEntryModified(entry);
        pending = entry;
    }


    public void generateIndex() throws DepotWriteException {
        // TODO: move to backend
        File feedDir = new File(
                depot.baseDir, depot.atomizer.getFeedPath());
        if (!feedDir.exists()) {
            feedDir.mkdir();
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

    /*public*/ void indexEntries(DepotEntryBatch entryBatch)
            throws DepotWriteException {
        try {
            depot.atomizer.indexEntries(entryBatch);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }

    /*public*/ DepotEntryBatch makeEntryBatch() {
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

    public void rollbackPending() throws DepotWriteException {
        if (pending == null) {
            throw new IllegalStateException(
                    "Cannot rollback: nothing is pending.");
        }
        pending.rollback();
        pending = null;
    }

    protected void commitPending() {
        if (pending != null) {
            // TODO: lazy make here, or preferably a more stable indexing!
            if (batch == null) {
                batch = makeEntryBatch();
            }
            batch.add(pending);
            pending.unlock();
        }
    }

    protected void onEntryModified(DepotEntry depotEntry)
            throws DepotWriteException {
        try {
            depot.atomizer.generateAtomEntryContent(depotEntry);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
        // TODO:? update latest feed index file (may create new file and modify
        // next-to-last (add next-archive)?)! Since any modifying means
        // a new updated depotEntry in the feeds..
    }

}
