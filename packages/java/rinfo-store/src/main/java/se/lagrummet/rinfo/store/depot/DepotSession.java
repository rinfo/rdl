package se.lagrummet.rinfo.store.depot;

import java.util.Date;
import java.util.List;
import java.net.URI;


public interface DepotSession {

    DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents)
            throws DepotReadException, DepotWriteException;
    DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures)
            throws DepotReadException, DepotWriteException;

    public void update(DepotEntry entry, Date updated,
            List<SourceContent> contents)
            throws DepotReadException, DepotWriteException;
    public void update(DepotEntry entry, Date updated,
            List<SourceContent> contents,
            List<SourceContent> enclosures)
            throws DepotReadException, DepotWriteException;

    public void delete(DepotEntry entry, Date deleted)
            throws DeletedDepotEntryException,
                   DepotReadException, DepotWriteException;

    public void close() throws DepotWriteException;
    public void rollbackPending() throws DepotWriteException;

}
