package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
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
import org.apache.abdera.ext.sharing.Sync;

import javax.xml.namespace.QName;


public class Atomizer {

    public static final String CONF_BASE_KEY = "rinfo.depot.atom.";

    // TODO: in Abdera somewhere? Or get from depot(.pathProcessor)?
    public static final String ATOM_ENTRY_MEDIA_TYPE = "application/atom+xml;type=entry";

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

    private FileDepot depot;

    private int feedBatchSize;
    private boolean includeDeleted;
    private boolean includeHistorical;
    private boolean useEntrySelfLink;
    private boolean useLinkExtensionsMd5;
    private boolean useTombstones;
    private boolean useFeedSync;
    private boolean useGdataDeleted;
    private boolean prettyXml;

    private Feed skeletonFeed;


    public Atomizer(FileDepot depot) {
        this.depot = depot;
    }

    public void configure(AbstractConfiguration config)
            throws ConfigurationException, FileNotFoundException {
        setFeedSkeleton(
                config.getString(CONF_BASE_KEY+"feedSkeleton"));
        setFeedBatchSize(
                config.getInt(CONF_BASE_KEY+"feedBatchSize", 0));
        setIncludeDeleted(
                config.getBoolean(CONF_BASE_KEY+"includeDeleted", true));
        setIncludeHistorical( // TODO: true
                config.getBoolean(CONF_BASE_KEY+"includeHistorical", false));
        setUseEntrySelfLink(
                config.getBoolean(CONF_BASE_KEY+"useEntrySelfLink", true));
        setUseLinkExtensionsMd5(
                config.getBoolean(CONF_BASE_KEY+"useLinkExtensionsMd5", true));
        setUseTombstones(
                config.getBoolean(CONF_BASE_KEY+"useTombstones", true));
        setUseFeedSync(
                config.getBoolean(CONF_BASE_KEY+"useFeedSync", true));
        setUseGdataDeleted(
                config.getBoolean(CONF_BASE_KEY+"useGdataDeleted", true));
        // TODO:IMPROVE: remove support for prettyXml? In 0.4, it's still too
        // brittle (accumulates whitespace over time).
        setPrettyXml(
                config.getBoolean(CONF_BASE_KEY+"prettyXml", false));
    }

    public FileDepot getDepot() { return depot; }

    public int getFeedBatchSize() {
        return feedBatchSize!=0 ? feedBatchSize : DEFAULT_FEED_BATCH_SIZE;
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

    void setFeedSkeleton(String feedSkeleton) throws FileNotFoundException {
        if (feedSkeleton!=null && !feedSkeleton.equals("")) {
            skeletonFeed = (Feed) Abdera.getInstance().getParser().parse(
                    new FileInputStream(feedSkeleton)).getRoot();
        }
    }


    //== Feed Specifics ==

    public void generateIndex() throws IOException {
        File feedDir = new File(depot.getBaseDir(), depot.getFeedPath());
        if (!feedDir.exists()) {
            feedDir.mkdir();
        }
        DepotEntryBatch entryBatch = depot.makeEntryBatch();

        for (Iterator<DepotEntry> iter = depot.iterateEntries(
                includeHistorical, includeDeleted); iter.hasNext(); ) {
            entryBatch.add(iter.next());
        }

        FileUtils.cleanDirectory(feedDir);
        indexEntries(entryBatch);
    }

    // TODO: test algorithm in isolation! (refactor?)
    // .. perhaps with overridden generateAtomEntryContent + writeFeed
    public void indexEntries(DepotEntryBatch entryBatch) throws IOException {

        String subscriptionPath = depot.getSubscriptionPath();

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
                String archPath = depot.pathToArchiveFeed(depotEntry.getUpdated()); // youngest entry..
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

            indexEntry(currFeed, depotEntry);

        }
        writeFeed(currFeed); // as subscription feed

    }

    protected Feed newFeed(String uriPath) {
        Feed feed;
        if (skeletonFeed != null) {
            feed = (Feed) skeletonFeed.clone();
        } else {
            feed = Abdera.getInstance().newFeed();
        }
        feed.setUpdated(new Date()); // TODO:IMPROVE: always use "now" utcDateTime?
        feed.addLink(uriPath, "self");
        return feed;
    }

    protected Feed getFeed(String uriPath) {
        File file = new File(depot.getBaseDir(), depot.toFeedFilePath(uriPath));
        try {
            return (Feed) Abdera.getInstance().getParser().parse(
                    new FileInputStream(file)).getRoot();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    protected Feed getPrevArchiveAsFeed(Feed feed) {
        IRI prev = FeedPagingHelper.getNextArchive(feed);
        if (prev==null) {
            return null;
        }
        return getFeed(prev.toString());
    }

    /* TODO: to use for e.g. "emptying" deleted entries
        - search in feed folder by date, time; opt. offset (if many of same in same instant?)
    protected Feed getFeedForDateTime(Date date) {
        .. getFeedForDateTime(depot.pathToArchiveFeed(date))
        return null;
    }
    */

    protected String uriPathFromFeed(Feed feed) {
        return feed.getSelfLink().getHref().toString();
    }

    protected void writeFeed(Feed feed) throws IOException, FileNotFoundException {
        String uriPath = uriPathFromFeed(feed);
        logger.info("Writing feed: <"+uriPath+">");
        String feedFileName = depot.toFeedFilePath(uriPath);
        File feedFile = new File(depot.getBaseDir(), feedFileName);
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
    }

    protected void indexEntry(Feed feed, DepotEntry depotEntry)
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
            /* TODO: Dry out, unless generating new (when we know all, incl. deleteds..)
                If so, historical entries must know if their current is deleted!
            dryOutHistoricalEntries(depotEntry)
            */
        }
        /* TODO: Ensure this insert is only done if atomEntry represents
            a deletion in itself (how to combine with addTombstone?).
        if (useDeletedEntriesInFeed) ...
        */
        Entry atomEntry = generateAtomEntryContent(depotEntry, false);
        atomEntry.setSource(null);
        feed.insertEntry(atomEntry);
    }


    //== Entry Specifics ==

    public Entry generateAtomEntryContent(DepotEntry depotEntry)
            throws IOException, FileNotFoundException {
        return generateAtomEntryContent(depotEntry, true);
    }

    public Entry generateAtomEntryContent(DepotEntry depotEntry, boolean force)
            throws IOException, FileNotFoundException {
        File entryFile = depotEntry.newContentFile(ATOM_ENTRY_MEDIA_TYPE);
        if (!force &&
            entryFile.isFile() &&
            entryFile.lastModified() > depotEntry.lastModified()) {
            return (Entry) Abdera.getInstance().getParser().parse(
                    new FileInputStream(entryFile)).getRoot();
        }
        Entry atomEntry = createAtomEntry(depotEntry);
        OutputStream outStream = new FileOutputStream(entryFile);
        if (prettyXml) {
            atomEntry.writeTo("prettyxml", outStream);
        } else {
            atomEntry.writeTo(outStream);
        }
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

        if (skeletonFeed != null) {
            atomEntry.setSource(skeletonFeed);
        }

        if (useFeedSync) {
            // TODO: paint history
        }

        if (depotEntry.isDeleted()) {
            markAsDeleted(atomEntry);
            return atomEntry;
        }

        // TODO: what to use as values?
        atomEntry.setTitle("");//getId().toString())
        atomEntry.setSummary("");//getId().toString())

        if (useEntrySelfLink) {
            String selfUriPath = depot.getPathProcessor().makeNegotiatedUriPath(
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


    //== Utilities ==

    public static Map<IRI, AtomDate> getDeletedMarkers(Feed feed)
            throws URISyntaxException {
        Map deletedMap = new HashMap();
        List<Element> tombstones = feed.getExtensions(FEED_EXT_TOMBSTONE);
        for (Element elem : tombstones) {
            deletedMap.put(new IRI(elem.getAttributeValue(TOMBSTONE_REF)),
                    new AtomDate(elem.getAttributeValue(TOMBSTONE_WHEN)));
        }
        for (Entry entry : feed.getEntries()) {
            if (isDeleted(entry)) {
                deletedMap.put(entry.getId(),
                        entry.getUpdatedElement().getValue());
            }
        }
        return deletedMap;
    }

    public static boolean isDeleted(Entry entry) {
        if (entry.getExtension(ENTRY_EXT_GDATA_DELETED) != null) {
            return true;
        }
        Sync sync = SharingHelper.getSync(entry);
        if (sync != null) {
            return sync.isDeleted();
        }
        return false;
    }

}
