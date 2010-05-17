package se.lagrummet.rinfo.store.depot;

import java.io.InputStream;
import java.io.OutputStream;
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

    OutputStream getMetaOutputStream(String resourceName)
        throws DepotWriteException;
    InputStream getMetaInputStream(String resourceName)
        throws DepotReadException;

    void create(Date createTime, List<SourceContent> sourceContents)
        throws DepotWriteException;
    void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures)
        throws DepotWriteException;
    void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures, boolean releaseLock)
        throws DepotWriteException;

    void update(Date updateTime, List<SourceContent> sourceContents)
        throws DepotWriteException;
    void update(Date updateTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures)
        throws DepotWriteException;

    void delete(Date deleteTime)
        throws DeletedDepotEntryException, DepotIndexException, DepotWriteException;

    void lock() throws DepotWriteException;
    void unlock() throws DepotWriteException;
    void rollback() throws DepotWriteException;
    boolean hasHistory();
    void wipeout() throws DepotWriteException, DepotIndexException;

}
