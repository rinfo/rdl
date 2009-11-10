package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.AtomDate;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Link;
import org.apache.abdera.i18n.iri.IRI;

import org.apache.abdera.ext.history.FeedPagingHelper;
import org.apache.abdera.ext.sharing.SharingHelper;


public class Atomizer {

    // TODO:? Get from (or use in?) depot(.pathHandler)?
    public static final String ATOM_ENTRY_MEDIA_TYPE = "application/atom+xml;type=entry";

    // TODO:IMPROVE: depend on base and use base.atom.AtomEntryDeleteUtil?

    public static final QName ENTRY_EXT_GDATA_DELETED = new QName(
            "http://schemas.google.com/g/2005", "deleted", "gd");

    public static final QName LINK_EXT_MD5 = new QName(
            "http://purl.org/atompub/link-extensions/1.0", "md5", "le");

    public static final QName FEED_EXT_TOMBSTONE = new QName(
            "http://purl.org/atompub/tombstones/1.0", "deleted-entry", "at");
    public static final String TOMBSTONE_REF = "ref";
    public static final String TOMBSTONE_WHEN = "when";

    public static final int DEFAULT_FEED_BATCH_SIZE = 25;


    private final Logger logger = LoggerFactory.getLogger(Atomizer.class);

    /* TODO: factor out dep to depot?
       .. minimal path- and write operations

        depot.getPathHandler().makeNegotiatedUriPath(...)

        depot.backend.getFeedFile(uriPath)

       .. Better:
        - have depot contain:
            - pathHandler (swappable or just configurable?)
            - Backend interface:
                handles all read and write operations, including toFilePath(logicalPath)
            - Indexer interface: the "pure algorithm" (lazy if backend is db?)
                .. DepotWriter to do all create+update (state with currentFeed etc)?
            - Atomizer: left to do syntax only
    */
    private FileDepot depot;

    private String feedPath;

    private int feedBatchSize = DEFAULT_FEED_BATCH_SIZE;
    private boolean includeDeleted = true;
    private boolean includeHistorical = false;
    private boolean useEntrySelfLink = true;
    private boolean useLinkExtensionsMd5 = true;
    private boolean useTombstones = true;
    private boolean useFeedSync = true;
    private boolean useGdataDeleted = true;
    // TODO:IMPROVE: remove support for prettyXml? In Abdera 0.4, it's still
    // too brittle (accumulates whitespace over time).
    private boolean prettyXml = false;

    private String feedSkeletonPath;
    private Feed feedSkeleton;


    public Atomizer() {
    }

    public Atomizer(FileDepot depot) {
        setDepot(depot);
    }

    public FileDepot getDepot() { return depot; }
    public void setDepot(FileDepot depot) {
        this.depot = depot;
    }

    public String getFeedPath() { return feedPath; }
    public void setFeedPath(String feedPath) {
        this.feedPath = feedPath;
    }

    public String getSubscriptionPath() {
        // TODO: configurable? Settable "feedSubscriptionSegment"?
        return feedPath+"/current";
    }

    public String pathToArchiveFeed(Date youngestDate) {
        String archPath = DatePathUtil.toFeedArchivePath(youngestDate);
        return feedPath+"/"+archPath;
    }


    public int getFeedBatchSize() {
        return feedBatchSize;
    }
    public void setFeedBatchSize(int feedBatchSize) {
        this.feedBatchSize = feedBatchSize;
    }

    public boolean getIncludeDeleted() { return includeDeleted; }
    public void setIncludeDeleted(boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
    }

    public boolean getIncludeHistorical() { return includeHistorical; }
    public void setIncludeHistorical(boolean includeHistorical) {
        this.includeHistorical = includeHistorical;
    }

    public boolean getUseEntrySelfLink() { return useEntrySelfLink; }
    public void setUseEntrySelfLink(boolean useEntrySelfLink) {
        this.useEntrySelfLink = useEntrySelfLink;
    }

    public boolean getUseLinkExtensionsMd5() { return useLinkExtensionsMd5; }
    public void setUseLinkExtensionsMd5(boolean useLinkExtensionsMd5) {
        this.useLinkExtensionsMd5 = useLinkExtensionsMd5;
    }

    public boolean getUseTombstones() { return useTombstones; }
    public void setUseTombstones(boolean useTombstones) {
        this.useTombstones = useTombstones;
    }

    public boolean getUseFeedSync() { return useFeedSync; }
    public void setUseFeedSync(boolean useFeedSync) {
        this.useFeedSync = useFeedSync;
    }

    public boolean getUseGdataDeleted() { return useGdataDeleted; }
    public void setUseGdataDeleted(boolean useGdataDeleted) {
        this.useGdataDeleted = useGdataDeleted;
    }

    public boolean getPrettyXml() { return prettyXml; }
    public void setPrettyXml(boolean prettyXml) {
        this.prettyXml = prettyXml;
    }

    public boolean isUsingEntriesAsTombstones() {
        return getUseFeedSync() || getUseGdataDeleted();
    }

    public String getFeedSkeletonPath() { return feedSkeletonPath; }
    void setFeedSkeletonPath(String feedSkeletonPath) throws IOException {
        this.feedSkeletonPath = feedSkeletonPath;
        if (feedSkeletonPath != null && !feedSkeletonPath.equals("")) {
            feedSkeleton = (Feed) Abdera.getInstance().getParser().parse(
                    ConfigurationUtils.locate(feedSkeletonPath).openStream()
                ).getRoot();
        }
    }

    public Feed getFeedSkeleton() { return feedSkeleton; }
    public void setFeedSkeleton(Feed feedSkeleton) {
        this.feedSkeletonPath = null;
        this.feedSkeleton = feedSkeleton;
    }


    //== Feed Specifics ==

    // TODO: test algorithm in isolation! (refactor?)
    // .. perhaps with overridden indexEntry, getFeed, writeFeed..?
    public void indexEntries(DepotEntryBatch entryBatch)
            throws DepotWriteException, IOException {
        // TODO:? create a LOCKED file in the feed dir

        String subscriptionPath = getSubscriptionPath();

        Feed currFeed = getFeed(subscriptionPath);
        if (currFeed == null) {
            currFeed = newFeed(subscriptionPath);
        }
        Feed youngestArchFeed = getPrevArchiveAsFeed(currFeed);

        int batchCount = currFeed.getEntries().size();
        Date currentDate = null;
        if (batchCount > 0) {
            currentDate = currFeed.getEntries().get(0).getUpdated();
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

            if (batchCount > getFeedBatchSize()) { // save as archive
                FeedPagingHelper.setArchive(currFeed, true);
                String archPath = pathToArchiveFeed(depotEntry.getUpdated()); // youngest entry..
                currFeed.getSelfLink().setHref(archPath);
                FeedPagingHelper.setCurrent(currFeed, subscriptionPath);
                if (youngestArchFeed!=null) {
                    FeedPagingHelper.setNextArchive(youngestArchFeed,
                            uriPathFromFeed(currFeed));
                    writeFeed(youngestArchFeed); // re-write..
                }
                writeFeed(currFeed);
                youngestArchFeed = currFeed;
                currFeed = newFeed(subscriptionPath);
                batchCount = 1; // current item ends up in the new feed
            }

            if (youngestArchFeed!=null) {
                FeedPagingHelper.setPreviousArchive(currFeed,
                        uriPathFromFeed(youngestArchFeed));
            }

            logger.info("Indexing entry: <"+depotEntry.getId()+"> ["+depotEntry.getUpdated()+"]");

            indexEntry(currFeed, (FileDepotEntry) depotEntry);

        }
        writeFeed(currFeed); // as subscription feed

        // TODO:? remove LOCKED (see above)
    }

    public Feed getFeed(String uriPath) throws IOException {
        File feedFile = depot.backend.getFeedFile(uriPath);
        try {
            InputStream inStream = new FileInputStream(feedFile);
            Feed feed = (Feed) Abdera.getInstance().getParser().parse(
                    inStream).getRoot();
            feed.complete();
            inStream.close();
            return feed;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public Feed getPrevArchiveAsFeed(Feed feed) throws IOException {
        IRI prev = FeedPagingHelper.getPreviousArchive(feed);
        if (prev == null) {
            return null;
        }
        return getFeed(prev.toString());
    }

    public String uriPathFromFeed(Feed feed) {
        return feed.getSelfLink().getHref().toString();
    }

    protected Feed newFeed(String uriPath) {
        Feed feed;
        if (feedSkeleton != null) {
            feed = (Feed) feedSkeleton.clone();
        } else {
            feed = Abdera.getInstance().newFeed();
        }
        feed.setUpdated(new Date()); // TODO:? always use "now" utcDateTime?
        feed.addLink(uriPath, "self");
        return feed;
    }

    /* TODO: to use for e.g. "emptying" deleted entries.
        Search in feed folder by date, time; opt. offset (if many of same in
        same instant?).
    protected Feed findFeedForDateTime(Date date) {
        .. findFeedForDateTime(pathToArchiveFeed(date))
        return null;
    }
    */

    protected void writeFeed(Feed feed) throws IOException, FileNotFoundException {
        String uriPath = uriPathFromFeed(feed);
        logger.info("Writing feed: <"+uriPath+">");
        File feedFile = depot.backend.getFeedFile(uriPath);
        File feedDir = feedFile.getParentFile();
        if (!feedDir.exists()) {
            FileUtils.forceMkdir(feedDir);
        }
        OutputStream outStream = new FileOutputStream(feedFile);
        if (prettyXml) {
            feed.writeTo("prettyxml", outStream);
        } else {
            feed.writeTo(outStream);
        }
        outStream.close();
    }

    protected void indexEntry(Feed feed, FileDepotEntry depotEntry)
            throws IOException, FileNotFoundException {
        if (depotEntry.isDeleted()) {
            if (useTombstones) {
                Element delElem = feed.addExtension(FEED_EXT_TOMBSTONE);
                delElem.setAttributeValue(
                        TOMBSTONE_REF, depotEntry.getId().toString());
                delElem.setAttributeValue(
                        TOMBSTONE_WHEN,
                        new AtomDate(depotEntry.getUpdated()).getValue());
            }
            /* TODO:IMPROVE:
                Dry out, unless generating new (when we know all, incl. deleteds..)
                If so, historical entries must know if their current is deleted!
            dryOutHistoricalEntries(depotEntry)
            */
        }
        // NOTE: Test to ensure this insert is only done if atomEntry represents
        //  a deletion in itself (and not only feed-level tombstone markers).
        if (!depotEntry.isDeleted() || isUsingEntriesAsTombstones()) {
            Entry atomEntry = generateAtomEntryContent(depotEntry, false);
            atomEntry.setSource(null);
            feed.insertEntry(atomEntry);
        }
    }


    //== Entry Specifics ==

    public Entry generateAtomEntryContent(DepotEntry depotEntry)
            throws IOException {
        return generateAtomEntryContent((FileDepotEntry) depotEntry, true);
    }

    public Entry generateAtomEntryContent(FileDepotEntry depotEntry, boolean force)
            throws IOException {
        File entryFile = depotEntry.newContentFile(ATOM_ENTRY_MEDIA_TYPE);
        if (!force &&
            entryFile.isFile() &&
            entryFile.lastModified() > depotEntry.lastModified()) {
            InputStream inStream = new FileInputStream(entryFile);
            Entry entry = (Entry) Abdera.getInstance().getParser().parse(
                    inStream).getRoot();
            entry.complete();
            inStream.close();
            return entry;
        }
        Entry atomEntry = createAtomEntry(depotEntry);
        OutputStream outStream = new FileOutputStream(entryFile);
        if (prettyXml) {
            atomEntry.writeTo("prettyxml", outStream);
        } else {
            atomEntry.writeTo(outStream);
        }
        outStream.close();
        return atomEntry;
    }

    protected Entry createAtomEntry(DepotEntry depotEntry)
            throws IOException, FileNotFoundException {

        Entry atomEntry = Abdera.getInstance().newEntry();
        if (useLinkExtensionsMd5) {
            atomEntry.declareNS(
                    LINK_EXT_MD5.getNamespaceURI(), LINK_EXT_MD5.getPrefix());
        }

        atomEntry.setId(depotEntry.getId().toString());
        atomEntry.setUpdated(depotEntry.getUpdated());
        Date publDate = depotEntry.getPublished();
        if (publDate!=null) {
            atomEntry.setPublished(publDate);
        }

        if (feedSkeleton != null) {
            atomEntry.setSource(feedSkeleton);
        }

        if (useFeedSync) {
            // TODO: paint history
        }

        if (depotEntry.isDeleted()) {
            markAsDeleted(atomEntry);
            return atomEntry;
        }

        // TODO: what to use as values?
        atomEntry.setTitle(depotEntry.getId().toString());
        atomEntry.setSummary("");//getId().toString()

        if (useEntrySelfLink) {
            String selfUriPath = depot.getPathHandler().makeNegotiatedUriPath(
                    depotEntry.getEntryUriPath(), ATOM_ENTRY_MEDIA_TYPE);
            atomEntry.addLink(selfUriPath, "self");
        }

        addContents(depotEntry, atomEntry);
        addEnclosures(depotEntry, atomEntry);

        return atomEntry;
    }

    protected void markAsDeleted(Entry atomEntry) {
        if (useGdataDeleted) {
            atomEntry.addExtension(ENTRY_EXT_GDATA_DELETED);
        }
        if (useFeedSync) {
            SharingHelper.deleteEntry(atomEntry, ""); // TODO: by..
        }
        atomEntry.setTitle("");
        atomEntry.setContent("");
    }

    protected void addContents(DepotEntry depotEntry, Entry atomEntry)
            throws IOException, FileNotFoundException {
        boolean contentIsSet = false;
        for (DepotContent content : depotEntry.findContents()) {
            if (content.getMediaType().equals(ATOM_ENTRY_MEDIA_TYPE)) {
                continue;
            }
            Element ref;
            if (!contentIsSet
                    && ObjectUtils.equals(content.getMediaType(), depotEntry.getContentMediaType())
                    && ObjectUtils.equals(content.getLang(), depotEntry.getContentLanguage())
                ) {
                ref = atomEntry.setContent(new IRI(content.getDepotUriPath()),
                        content.getMediaType());
                if (content.getLang() != null) {
                    atomEntry.getContentElement().setLanguage(content.getLang());
                }
                contentIsSet = true;
            } else {
                ref = atomEntry.addLink(content.getDepotUriPath(),
                        "alternate",
                        content.getMediaType(),
                        null, // title
                        content.getLang(),
                        content.getFile().length());
            }
            if (useLinkExtensionsMd5) {
                ref.setAttributeValue(LINK_EXT_MD5, content.getMd5Hex());
            }
        }
    }

    protected void addEnclosures(DepotEntry depotEntry, Entry atomEntry)
            throws IOException {
        for (DepotContent enclContent : depotEntry.findEnclosures()) {
            Link link = atomEntry.addLink(enclContent.getDepotUriPath(),
                    "enclosure",
                    enclContent.getMediaType(),
                    null, // title
                    enclContent.getLang(),
                    enclContent.getFile().length());
            if (useLinkExtensionsMd5) {
                link.setAttributeValue(LINK_EXT_MD5, enclContent.getMd5Hex());
            }
        }
    }

}
