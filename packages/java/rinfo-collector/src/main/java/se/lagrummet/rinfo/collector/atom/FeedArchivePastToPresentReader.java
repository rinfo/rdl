package se.lagrummet.rinfo.collector.atom;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.abdera.model.AtomDate;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;
import org.apache.abdera.i18n.iri.IRI;


/**
 * A FeedArchiveReader guaranteed to track backwards in time through feed pages,
 * examining each entry in youngest to oldest order per page by calling
 * {@link stopOnEntry}. When completed, it will re-read all visited pages
 * in turn and process them in chronological order from oldest (known) to
 * youngest.
 *
 * <em>Warning!</em> Instances of this class are not thread safe.
 */
public abstract class FeedArchivePastToPresentReader extends FeedArchiveReader {

    private final Logger logger = LoggerFactory.getLogger(
            FeedArchivePastToPresentReader.class);

    private LinkedList<FeedReference> feedTrail;
    private Map<IRI, AtomDate> entryModificationMap;
    private Entry knownStoppingEntry;

    @Override
    public void beforeTraversal() {
        feedTrail = new LinkedList<FeedReference>();
        entryModificationMap = new HashMap<IRI, AtomDate>();
        knownStoppingEntry = null;
    }

    @Override
    public void afterTraversal() throws URISyntaxException {
        for (FeedReference feedRef : feedTrail) {
            try {
                try {
                    Feed feed = feedRef.openFeed();
                    feed = feed.sortEntriesByUpdated(false);

                    Map<IRI, AtomDate> deletedMap =
                            AtomEntryDeleteUtil.getDeletedMarkers(feed);
                    List<Entry> effectiveEntries = new ArrayList<Entry>();

                    for (Entry entry: feed.getEntries()) {
                        IRI entryId = entry.getId();
                        if (deletedMap.containsKey(entryId)) {
                            continue;
                        }
                        Date entryUpdated = entry.getUpdated();
                        AtomDate youngestAtomDate = entryModificationMap.get(
                                entryId);
                        boolean notSeenOrYoungestOfSeen =
                                youngestAtomDate == null ||
                                youngestAtomDate.getDate().equals(entryUpdated);
                        if (notSeenOrYoungestOfSeen) {
                            boolean knownOrOlderThanKnown =
                                knownStoppingEntry != null && (
                                    entryId.equals(
                                        knownStoppingEntry.getId()) ||
                                    isOlderThan(entryUpdated,
                                        knownStoppingEntry.getUpdated()));
                            if (knownOrOlderThanKnown) {
                                continue;
                            }
                            effectiveEntries.add(entry);
                        }
                    }
                    processFeedPageInOrder(feedRef.getFeedUrl(), feed,
                            effectiveEntries, deletedMap);
                } finally {
                    feedRef.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        // NOTE: cleanup trail if an exception occurred in afterTraversal.
        for (FeedReference feedRef : feedTrail) {
            try {
                feedRef.close();
            } catch (IOException e) {
                logger.error("Could not close " + feedRef, e);
            }
        }
    }

    @Override
    public URL readFeedPage(URL url) throws IOException {
        if (hasVisitedArchivePage(url)) {
            logger.info("Stopping on visited archive page: <"+url+">");
            return null;
        } else {
            return super.readFeedPage(url);
        }
    }

    @Override
    public boolean processFeedPage(URL pageUrl, Feed feed) throws Exception {
        feedTrail.addFirst(new FeedReference(pageUrl, feed));
        feed = feed.sortEntriesByUpdated(true);

        Map<IRI, AtomDate> deletedMap = getDeletedMarkers(feed);
        for (Map.Entry<IRI, AtomDate> item : deletedMap.entrySet()) {
            putUriDateIfNewOrYoungest(entryModificationMap, item.getKey(), item.getValue());
        }

        // FIXME:? needs to scan the rest with the same updated stamp before
        // stopping (even if this means following more pages back in time?)?
        // TODO: It would thus also be wise to mark/remove entries in feedTrail
        // which have been visited (so the subclass don't have to check this
        // twice).
        for (Entry entry : feed.getEntries()) {
            if (stopOnEntry(entry)) {
                logger.info("Stopping on known entry: <" +entry.getId() +
                        "> ["+entry.getUpdatedElement().getString()+"]");
                knownStoppingEntry = entry;
                return false;
            }
            putUriDateIfNewOrYoungest(entryModificationMap,
                    entry.getId(),
                    entry.getUpdatedElement().getValue());
        }

        return true;
    }

    /**
     * Template method intended for the actual feed processing.
     * This method is guaranteed to be called in sequence from oldest page
     * to newest, with a feed entries sorted in chronological order <em>from
     * oldest to newest</em>. Note that the feed will contain <em>all</em>
     * entries, even the ones older than any known processed entry.
     *
     * @param pageUrl The URL of the feed page.
     * @param feed The feed itself (with entries sorted in chronological order).
     *
     * @param effectiveEntries Entries in the current feed, filtered so that:
     * <ul>
     *   <li>No younger entries exist in the range of collected feed pages.</li>
     *   <li>The entry has no tombstone in the current feed.</li>
     * </ul>
     *
     * @param deletedMap A map of tombstones (given in one of the forms
     *        supported by {@link getDeletedMarkers}).
     */
    public abstract void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deletedMap);

    /**
     * Template method to stop on known feed entry.
     * @return whether to continue climbing backwards in time collecting feed
     *         pages to process.
     */
    public abstract boolean stopOnEntry(Entry entry);

    /**
     * Default method to stop on known visited feed archive pages.
     * @return whether to read the page or not. Default always returns false.
     */
    public boolean hasVisitedArchivePage(URL pageUrl) {
        return false;
    }

    /**
     * Default method used to get tombstone markers from a feed.
     * @return A map of entry id:s and deletion times. The default uses {@link
     *         AtomEntryDeleteUtil.getDeletedMarkers}.
     */
    public Map<IRI, AtomDate> getDeletedMarkers(Feed feed)
            throws URISyntaxException {
        return AtomEntryDeleteUtil.getDeletedMarkers(feed);
    }

    public static boolean isYoungerThan(Date date, Date thanDate) {
        return date.compareTo(thanDate) > 0;
    }

    public static boolean isOlderThan(Date date, Date thanDate) {
        return date.compareTo(thanDate) < 0;
    }

    static boolean putUriDateIfNewOrYoungest(Map<IRI, AtomDate> map,
            IRI iri, AtomDate atomDate) {
        AtomDate storedAtomDate = map.get(iri);
        if (storedAtomDate != null) {
            Date date = atomDate.getDate();
            Date storedDate = storedAtomDate.getDate();
            // keep largest date => ignore all older (smaller)
            if (isOlderThan(atomDate.getDate(), storedAtomDate.getDate())) {
                return false;
            }
        }
        map.put(iri, atomDate);
        return true;
    }

    public static class FeedReference {

        private URL feedUrl;
        private URI tempFileUri;
        private InputStream tempInStream;

        public FeedReference(URL feedUrl, Feed feed)
                throws IOException, FileNotFoundException {
            this.feedUrl = feedUrl;
            File tempFile = File.createTempFile("feed", ".atom");
            tempFileUri = tempFile.toURI();
            OutputStream outStream = new FileOutputStream(tempFile);
            feed.writeTo(outStream);
            outStream.close();
        }

        public URL getFeedUrl() {
            return feedUrl;
        }

        public Feed openFeed() throws IOException, FileNotFoundException {
            tempInStream = new FileInputStream(getTempFile());
            Feed feed = parseFeed(tempInStream, feedUrl);
            return feed;
        }

        public void close() throws IOException {
            if (tempInStream != null) {
              tempInStream.close();
              tempInStream = null;
            }
            File tempFile = getTempFile();
            if (tempFile.exists()) {
              tempFile.delete();
            }
        }

        public String toString() {
            return "FeedReference(feedUrl="+this.feedUrl +
                    ", tempFileUri="+this.tempFileUri+")";
        }

        private File getTempFile() {
            return new File(tempFileUri);
        }

    }

}
