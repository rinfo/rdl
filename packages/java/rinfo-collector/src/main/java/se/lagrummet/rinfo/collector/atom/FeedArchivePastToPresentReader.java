package se.lagrummet.rinfo.collector.atom;

import java.io.*;
import java.util.*;
import java.net.URI;
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

    @Override
    public void beforeTraversal() {
        feedTrail = new LinkedList<FeedReference>();
        entryModificationMap = new HashMap<IRI, AtomDate>();
    }

    @Override
    public void afterTraversal() {
        for (FeedReference feedRef : feedTrail) {
            try {
                try {
                    Feed feed = feedRef.getFeed();
                    feed = feed.sortEntriesByUpdated(false);
                    /* FIXME: supply:
                        - entriesCurrentlyInEffect
                            if not in entryModificationMap or date.equals
                            and not older or equals stoppedOnKnownEntry
                        - deletedRefs
                    */
                    processFeedPageInOrder(feedRef.getFeedUrl(), feed);
                } finally {
                    feedRef.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
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

        Map<IRI, AtomDate> deletedMap = AtomEntryDeleteUtil.getDeletedMarkers(feed);
        for (Map.Entry<IRI, AtomDate> item : deletedMap.entrySet()) {
            putUriDateIfNewOrYoungest(entryModificationMap, item.getKey(), item.getValue());
        }

        for (Entry entry : feed.getEntries()) {
            if (stopOnEntry(entry)) {
                logger.info("Stopping on known entry: <" +entry.getId() +
                        "> ["+entry.getUpdatedElement().getString()+"]");
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
     */
    public abstract void processFeedPageInOrder(URL pageUrl, Feed feed);

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

    static boolean putUriDateIfNewOrYoungest(Map<IRI, AtomDate> map,
            IRI iri, AtomDate atomDate) {
        AtomDate storedAtomDate = map.get(iri);
        if (storedAtomDate != null) {
            Date date = atomDate.getDate();
            Date storedDate = storedAtomDate.getDate();
            // keep largest date => ignore all older (smaller)
            if(storedDate.compareTo(date) > 0) {
                return false;
            }
        }
        map.put(iri, atomDate);
        return true;
    }

    public static class FeedReference {

        private URL feedUrl;
        private URI tempFileUri;

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

        public Feed getFeed() throws IOException, FileNotFoundException {
            InputStream inStream = new FileInputStream(getTempFile());
            Feed feed = parseFeed(inStream, feedUrl);
            inStream.close();
            return feed;
        }

        public void close() throws IOException {
            getTempFile().delete();
        }

        private File getTempFile() {
            return new File(tempFileUri);
        }

    }

}
