package se.lagrummet.rinfo.store.depot;

import java.util.Iterator;
import java.util.List;
import java.net.URI;


public interface Depot {

    URI getBaseUri();
    void setBaseUri(URI baseUri);

    Atomizer getAtomizer();

    PathHandler getPathHandler();

    //void initialize();

    List find(String uriPath) throws DepotReadException;

    DepotEntry getEntry(URI entryUri) throws DepotReadException;
    DepotEntry getEntry(String uriPath) throws DepotReadException;

    DepotEntry getEntryOrDeletedEntry(URI entryUri) throws LockedDepotEntryException;
    DepotEntry getEntryOrDeletedEntry(String uriPath) throws LockedDepotEntryException;

    boolean hasEntry(URI entryUri);
    boolean hasEntry(String uriPath);
    boolean hasFeedView(String uriPath);

    DepotContent getContent(String uriPath);

    DepotSession openSession() throws DepotReadException;

    void generateIndex() throws DepotReadException, DepotWriteException;
    void checkConsistency() throws DepotReadException;

    Iterator iterateEntries();
    Iterator iterateEntries(boolean includeHistorical);
    Iterator iterateEntries(boolean includeHistorical, boolean includeDeleted);

}
