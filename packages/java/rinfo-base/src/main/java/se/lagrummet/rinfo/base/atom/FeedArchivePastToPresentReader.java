package se.lagrummet.rinfo.base.atom;

import java.io.*;
import java.util.*;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;


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

    private LinkedList<URL> feedTrail;
    private Map entryUpdatedMap;
    private URL firstArchiveUrl;

    @Override
    public void initialize() {
        super.initialize();
        firstArchiveUrl = null;
        feedTrail = new LinkedList<URL>();
        entryUpdatedMap = new HashMap();
    }

    @Override
    public void shutdown() {
        for (URL pageUrl : feedTrail) {
            try {
                Feed feed;
                InputStream inStream = getResponseAsInputStream(pageUrl);
                try {
                    feed = parseFeed(inStream, pageUrl);
                    feed = feed.sortEntriesByUpdated(false);
                    processFeedPageInOrder(pageUrl, feed);
                } finally {
                    inStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        super.shutdown();
    }

    @Override
    public URL readFeedPage(URL url) throws IOException {
        if (firstArchiveUrl == null) {
            firstArchiveUrl = url;
        }
        if (hasVisitedArchivePage(url)) {
            logger.info("Stopping on visited archive page: <"+url+">");
            return null;
        } else {
            return super.readFeedPage(url);
        }
    }

    @Override
    public boolean processFeedPage(URL pageUrl, Feed feed) {
        feedTrail.addFirst(pageUrl);
        feed = feed.sortEntriesByUpdated(true);
        for (Entry entry : feed.getEntries()) {
            if (stopOnEntry(entry)) {
                logger.info("Stopping on known entry: <" +entry.getId() +
                        "> ["+entry.getUpdatedElement().getString()+"]");
                return false;
            }
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

}
