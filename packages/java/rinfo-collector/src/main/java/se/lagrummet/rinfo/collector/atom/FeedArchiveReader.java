package se.lagrummet.rinfo.collector.atom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;


public abstract class FeedArchiveReader {

    private final Logger logger = LoggerFactory.getLogger(FeedArchiveReader.class);

    public static final String LINK_NEXT_ARCHIVE_REL = "next-archive";
    public static final String LINK_PREV_ARCHIVE_REL = "prev-archive";

    private HttpClient httpClient;

    /**
     * This default constructor uses a new {@link DefaultHttpClient} with no
     * special settings.
     */
    public FeedArchiveReader() {
        this(new DefaultHttpClient());
    }

    public FeedArchiveReader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public final HttpClient getClient() {
        return httpClient;
    }

    /**
     * Called before {@link readFeed}.
     */
    public void initialize() {
    }

    /**
     * Always called when {@link readFeed} has completed. This shuts down the
     * HttpClient.
     */
    public void shutdown() {
        getClient().getConnectionManager().shutdown();
        logger.info("Closed down the HTTP client connection manager.");
    }

    /**
     * Starts the feed archive climbing.
     */
    public final void readFeed(URL url) throws Exception {
        initialize();
        try {
            beforeTraversal();
            URL followingUrl = url;
            while (followingUrl != null) {
                followingUrl = readFeedPage(followingUrl);
                if (followingUrl != null) {
                    logger.info(".. following: <"+followingUrl+">");
                }
            }
            afterTraversal();
            logger.info("Done.");
        } finally {
            shutdown();
        }
    }

    /**
     * Called before {@link readFeed} begins page traversal. Does nothing by
     * default.
     */
    public void beforeTraversal() throws Exception {
    }

    /**
     * Called when {@link readFeed} has (successfully) traversed all feed
     * pages. Does nothing by default.
     */
    public void afterTraversal() throws Exception {
    }

    /**
     * <p>Parses the feed and delegates to {@link processFeedPage} for handling.
     * Then looks for contained archive links to continue climbing (backwards
     * in time).</p>
     * <p>Only override this if you need to e.g. stop climbing at a certain
     * known archive URL.</p>
     *
     * @return the previous archive URL, or null to stop climbing.
     */
    public URL readFeedPage(URL url) throws IOException {
        logger.info("Reading Feed <"+url+"> ...");
        Feed feed;
        URL followingUrl = null;
        /* TODO:IMPROVE:
            optional use of supplied "If-Modified-Since"/"ETag"..
            .. from template method(?): getFeedPageResponseCache().getLastReadTimeTag(url)..
         */
        InputStream inStream = getResponseAsInputStream(url);
        try {
            feed = parseFeed(inStream, url);

            if (processFeedPage(url, feed)) {
                IRI followingHref = feed.getLinkResolvedHref(getFollowingPageRel());
                if (followingHref != null) {
                    followingUrl = followingHref.toURL();
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing feed!", e);
            throw new RuntimeException(e); /* TODO: stop on error?
            followingUrl = null; */
        } finally {
            inStream.close();
        }
        return followingUrl;
    }

    /**
     * Default method for getting the link relation name of the following page
     * to read from the current feed page.
     *
     * @return Defaults to {@link LINK_PREV_ARCHIVE_REL}.
     */
    public String getFollowingPageRel() {
        return LINK_PREV_ARCHIVE_REL;
    }

    /**
     * Template method intended for the feed processing.
     * @return whether to continue backwards in time or stop.
     */
    public abstract boolean processFeedPage(URL pageUrl, Feed feed) throws Exception;

    public static Feed parseFeed(InputStream inStream, URL baseUrl) {
        return (Feed) Abdera.getInstance().getParser().parse(
                inStream, baseUrl.toString()).getRoot();
    }

    // TODO: define and make a default impl. (use a single dir, only latest feedUrl/feeId?)
    ///**
    // * Optional getter for a {@link FeedPageResponseCache}, used for
    // * conditional requests to optimize retrieval. See that interface for
    // * details.
    // * @return null by default.
    // */
    //public FeedPageResponseCache getFeedPageResponseCache() {
    //    return null;
    //}

    /**
     * Utility method to use the HttpClient (from {@link getClient}) to open
     * an URL and get the entity content as an InputStream.
     *
     * @return InputStream, or null if the response didn't enclose an entity.
     */
    public InputStream getResponseAsInputStream(URL url) throws IOException {
        return getResponseAsInputStream(url.toString());
    }

    public InputStream getResponseAsInputStream(String url) throws IOException {
        HttpGet urlGet = new HttpGet(url);
        HttpResponse response = getClient().execute(urlGet);
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }

}
