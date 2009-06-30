package se.lagrummet.rinfo.store.depot;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;


public interface DepotEntry {

    Depot getDepot();
    String getEntryUriPath();
    URI getId();
    Date getPublished();
    Date getUpdated();
    boolean isDeleted();
    boolean isLocked();
    String getContentMediaType();
    String getContentLanguage();

    long lastModified();
    void assertIsNotDeleted() throws DeletedDepotEntryException;
    void assertIsNotLocked() throws LockedDepotEntryException;

    List<DepotContent> findContents();
    List<DepotContent> findContents(String forMediaType);
    List<DepotContent> findContents(String forMediaType, String forLang);
    List<DepotContent> findEnclosures();

    File getMetaFile(String fileName);

    void create(Date createTime, List<SourceContent> sourceContents)
        throws DepotWriteException, IOException;
    void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures)
        throws DepotWriteException, IOException;
    void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures, boolean releaseLock)
        throws DepotWriteException, IOException;

    void update(Date updateTime, List<SourceContent> sourceContents)
        throws DepotWriteException, IOException;
    void update(Date updateTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures)
        throws DepotWriteException, IOException;

    void delete(Date deleteTime)
        throws DeletedDepotEntryException, DepotIndexException, IOException;

    void lock() throws IOException;
    void unlock() throws IOException;
    void rollback() throws DepotWriteException, IOException;
    boolean hasHistory();
    void wipeout() throws DepotIndexException, IOException;

}
