package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import javax.xml.namespace.QName;


public class Atomizer {

    private final Logger logger = LoggerFactory.getLogger(Atomizer.class);


    // TODO: in Abdera somewhere? Or get from depot(.pathProcessor)?
    public static final String ATOM_ENTRY_MEDIA_TYPE = "application/atom+xml;type=entry";

    public static final QName ENTRY_EXT_GDATA_DELETED = new QName(
            "http://schemas.google.com/g/2005", "deleted", "gd");

    public static final QName LINK_EXT_MD5 = new QName(
            "http://purl.org/atompub/link-extensions/1.0", "md5", "le");

    public static final QName FEED_EXT_TOMBSTONE = new QName(
            "http://purl.org/atompub/tombstones/1.0", "deleted-entry", "at");


    private FileDepot depot;
    public FileDepot getDepot() { return depot; }

    // TODO: Atomizer config properties
    private boolean includeDeleted = true;
    private boolean includeHistorical = false; // TODO: true

    private boolean useEntrySelfLink = true;
    private boolean useLinkExtensionsMd5 = true;
    private boolean useTombstones = true;
    private boolean useFeedSync = true;
    private boolean useGdataDeleted = true;
    //private String feedSkeleton;


    public Atomizer(FileDepot depot) {
        this.depot = depot;
    }

    public static final int DEFAULT_FEED_BATCH_SIZE = 25;

    private int feedBatchSize;
    public int getFeedBatchSize() {
        return feedBatchSize!=0 ? feedBatchSize : DEFAULT_FEED_BATCH_SIZE;
    }
    public void setFeedBatchSize(int feedBatchSize) {
        this.feedBatchSize = feedBatchSize;
    }


    private Feed skeletonFeed;
    void setFeedSkeleton(String feedSkeleton) throws FileNotFoundException {
        if (feedSkeleton!=null) {
            skeletonFeed = (Feed) Abdera.getInstance().getParser().parse(
                    new FileInputStream(feedSkeleton)).getRoot();
        }
    }

    public void generateIndex() throws IOException {
        File feedDir = new File(depot.getBaseDir(), depot.getFeedPath());
        if (!feedDir.exists()) {
            feedDir.mkdir();
        }
        Collection<DepotEntry>  entryBatch = makeEntryBatch();

        for (Iterator<DepotEntry> iter = depot.iterateEntries(
                includeHistorical, includeDeleted); iter.hasNext(); ) {
            entryBatch.add(iter.next());
        }

        FileUtils.cleanDirectory(feedDir);
        indexEntries(entryBatch);
    }

    public Collection<DepotEntry> makeEntryBatch() {
        return new DepotEntryBatch(depot, includeDeleted);
    }

    // TODO: test algorithm in isolation! (refactor?)
    // .. perhaps with overridden generateAtomEntryContent + writeFeed
    public void indexEntries(Collection<DepotEntry> entryBatch) throws IOException {

        String subscriptionPath = depot.getSubscriptionPath();

        Feed currFeed = getFeed(subscriptionPath);
        if (currFeed == null) {
            currFeed = newFeed(subscriptionPath);
        }
        Feed youngestArchFeed = getPrevArchiveAsFeed(currFeed);

        // FIXME: assure added entries are younger than latest in currFeed?


        int batchCount = currFeed.getEntries().size();
        Date currentDate = null;
        if (batchCount > 0) {
            currentDate = currFeed.getEntries().get(0).getUpdated();
        }

        for (DepotEntry depotEntry : entryBatch) {
            batchCount++;

            Date nextDate = depotEntry.getUpdated();
            if (currentDate != null) {
                assert nextDate.compareTo(currentDate) > 0;
                // TODO: ChronologyViolationException? or ever re-index..?
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
        feed.setUpdated(new Date()); // TODO: which utcDateTime?
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

    protected Feed getFeedForDateTime(Date date) {
        // TODO: to use for e.g. "emptying" deleted entries
        // - search in feed folder by date, time; opt. offset (can there be many in same same instant?)
        // .. getFeedForDateTime(depot.pathToArchiveFeed(date))
        return null;
    }

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
        feed.writeTo(new FileOutputStream(feedFile));
    }

    protected void indexEntry(Feed feed, DepotEntry depotEntry)
            throws IOException, FileNotFoundException {
        if (depotEntry.isDeleted()) {
            if (useTombstones) {
                Element delElem = feed.addExtension(FEED_EXT_TOMBSTONE);
                delElem.setAttributeValue(
                        "ref", depotEntry.getId().toString());
                delElem.setAttributeValue(
                        "when", new AtomDate(depotEntry.getUpdated()).getValue());
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
        atomEntry.writeTo(new FileOutputStream(entryFile));
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


    class DepotEntryBatch extends AbstractCollection<DepotEntry> {

        FileDepot depot;
        boolean includeDeleted;
        private TreeSet<EntryRef> ascDateSortedEntryRefs;

        public DepotEntryBatch(FileDepot depot, boolean includeDeleted) {
            this.depot = depot;
            this.includeDeleted = includeDeleted;
            ascDateSortedEntryRefs = new TreeSet<EntryRef>(
                    new Comparator<EntryRef>() {
                        public int compare(EntryRef a, EntryRef b) {
                            if (a.date.equals(b.date)) {
                                return a.uriPath.compareTo(b.uriPath);
                            }
                            return a.date.compareTo(b.date);
                        }
                });
        }

        public boolean add(DepotEntry depotEntry) {
            // only keeping necessary data to minimize memory use
            return ascDateSortedEntryRefs.add(new EntryRef(depotEntry));
        }

        public Iterator<DepotEntry> iterator() {
            final Iterator<EntryRef> sortedIter = ascDateSortedEntryRefs.iterator();
            return new Iterator<DepotEntry>() {
                public boolean hasNext() {
                    return sortedIter.hasNext();
                }
                public DepotEntry next() {
                    EntryRef entryRef = sortedIter.next();
                    try {
                        return depot.getEntry(entryRef.uriPath, !includeDeleted);
                    } catch (DeletedDepotEntryException e) {
                        throw new RuntimeException("Unexpected deleted entry.", e);
                    }
                }
                public void remove() {
                    sortedIter.remove();
                }
            };
        }

        public int size() {
            return ascDateSortedEntryRefs.size();
        }

    }

    private class EntryRef {
        public EntryRef(DepotEntry depotEntry) {
            this.uriPath = depotEntry.getEntryUriPath();
            this.date = depotEntry.getUpdated();
        }
        String uriPath;
        Date date;
    }


}
