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
    private DepotEntry pending;
    private AtomIndexer atomIndexer;

    public FileDepotSession(FileDepot depot) throws DepotReadException {
        this.depot = depot;
        atomIndexer = new AtomIndexer(depot.atomizer, depot.backend);
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
    }

    public void delete(DepotEntry entry, Date deleted)
            throws DeletedDepotEntryException,
                   DepotReadException, DepotWriteException {
        commitPending();
        pending = entry;
        entry.lock();
        entry.delete(deleted);
    }


    public void close() throws DepotWriteException {
        try {
            commitPending();
        } finally {
            atomIndexer.close();
        }
    }

    protected void commitPending() throws DepotWriteException {
        if (pending != null) {
            try {
                onChanged(pending);
                pending.unlock();
                pending = null;
            } catch (DepotWriteException e) {
                rollbackPending();
                throw e;
            }
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

    protected void onChanged(DepotEntry entry) throws DepotWriteException {
        atomIndexer.indexEntry(entry);
    }
}
