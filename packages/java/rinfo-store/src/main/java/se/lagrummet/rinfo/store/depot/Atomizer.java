package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.namespace.QName;

import org.apache.commons.configuration.ConfigurationUtils;
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


/**
 * A configurable tool for generating Atom content for DepotEntry objects,
 * including creating archive feeds and inserting updates and deletes.
 */
public class Atomizer {

    // TODO:? Get from pathHandler?
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

    private PathHandler pathHandler;

    public Atomizer() {
    }

    public Atomizer(PathHandler pathHandler) {
        setPathHandler(pathHandler);
    }

    public PathHandler getPathHandler() { return pathHandler; }
    public void setPathHandler(PathHandler pathHandler) {
        this.pathHandler = pathHandler;
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
    public void setFeedSkeletonPath(String feedSkeletonPath) throws IOException {
        this.feedSkeletonPath = feedSkeletonPath;
        if (feedSkeletonPath != null && !feedSkeletonPath.equals("")) {
            InputStream ins =
                ConfigurationUtils.locate(feedSkeletonPath).openStream();
            try {
                feedSkeleton = (Feed)
                    Abdera.getInstance().getParser().parse(ins).getRoot();
            } finally {
                ins.close();
            }
        }
    }

    public Feed getFeedSkeleton() { return feedSkeleton; }
    public void setFeedSkeleton(Feed feedSkeleton) {
        this.feedSkeletonPath = null;
        this.feedSkeleton = feedSkeleton;
    }


    //== Feed Specifics ==

    public void setArchive(Feed feed, boolean isArchive) {
        FeedPagingHelper.setArchive(feed, isArchive);
    }

    public void setNextArchive(Feed feed, String path) {
        FeedPagingHelper.setNextArchive(feed, path);
    }

    public IRI getPreviousArchive(Feed feed) {
        return FeedPagingHelper.getPreviousArchive(feed);
    }

    public void setPreviousArchive(Feed feed, String path) {
        FeedPagingHelper.setPreviousArchive(feed, path);
    }

    public void setCurrentFeedHref(Feed feed, String path) {
        FeedPagingHelper.setCurrent(feed, path);
    }

    public String uriPathFromFeed(Feed feed) {
        return feed.getSelfLink().getHref().toString();
    }


    public Feed newFeed(String uriPath) {
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


    public Entry addEntryToFeed(DepotEntry depotEntry, Feed feed)
            throws IOException {
        Entry atomEntry = null;
        if (depotEntry.isDeleted()) {
            if (useTombstones) {
                Element delElem = feed.addExtension(FEED_EXT_TOMBSTONE);
                delElem.setAttributeValue(
                        TOMBSTONE_REF, depotEntry.getId().toString());
                delElem.setAttributeValue(
                        TOMBSTONE_WHEN,
                        new AtomDate(depotEntry.getUpdated()).getValue());
            }
            //atomEntry = delElem; // TODO: use proposed atomdeleted repr?
        }
        // NOTE: Test to ensure this insert is only done if atomEntry represents
        //  a deletion in itself (and not only feed-level tombstone markers).
        if (!depotEntry.isDeleted() || isUsingEntriesAsTombstones()) {
            atomEntry = createAtomEntry(depotEntry);
            //atomEntry.setSource(null);
            feed.insertEntry(atomEntry);
        }
        return atomEntry;
    }


    //== Entry Specifics ==

    protected Entry createAtomEntry(DepotEntry depotEntry) throws IOException {
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
        atomEntry.setTitle(depotEntry.getId().toString());
        atomEntry.setSummary("");//getId().toString()

        if (useEntrySelfLink) {
            String selfUriPath = pathHandler.makeNegotiatedUriPath(
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
            throws IOException {
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
