package se.lagrummet.rinfo.store.depot;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;


public interface Depot {

    URI getBaseUri();
    void setBaseUri(URI baseUri);

    String getFeedPath();
    void setFeedPath(String feedPath);

    Atomizer getAtomizer();

    PathHandler getPathHandler();

    //void initialize();

    List find(String uriPath) throws DepotReadException;

    DepotEntry getEntry(URI entryUri) throws DepotReadException;
    DepotEntry getEntry(String uriPath) throws DepotReadException;

    Iterator iterateEntries();
    Iterator iterateEntries(boolean includeHistorical);
    Iterator iterateEntries(boolean includeHistorical, boolean includeDeleted);

    DepotContent getContent(String uriPath);

    boolean withinBaseUri(URI uri);

    DepotEntry createEntry(URI entryUri,
            Date created,
            List<SourceContent> contents)
        throws DepotReadException, DepotWriteException, IOException;
    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            boolean releaseLock)
            throws DepotReadException, DepotWriteException, IOException;
    DepotEntry createEntry(URI entryUri,
            Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures)
        throws DepotReadException, DepotWriteException, IOException;
    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures,
            boolean releaseLock)
            throws DepotReadException, DepotWriteException, IOException;

    void onEntryModified(DepotEntry depotEntry) throws IOException;

    void generateIndex() throws DepotWriteException, IOException;

    public void indexEntries(DepotEntryBatch entryBatch)
        throws DepotWriteException, IOException;

    DepotEntryBatch makeEntryBatch();

}
