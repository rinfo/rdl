package se.lagrummet.rinfo.base.atom;

import java.util.*;
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
import org.apache.abdera.model.Entry;


public abstract class FeedArchiveReader {

    private final Logger logger = LoggerFactory.getLogger(FeedArchiveReader.class);

    HttpClient httpClient;

    public final HttpClient getClient() {
        return httpClient;
    }

    /**
     * Called before {@link readFeed}. By default this sets the HttpClient from
     * {@link getClient} using {@createClient}.
     */
    public void initialize() {
         this.httpClient = createClient();
    }

    /**
     * Called after {@link readFeed}.
     */
    public void shutdown() {
    }

    /**
     * Overridable create method for an {@link HttpClient}. This defaults to
     * {@link DefaultHttpClient} with no special settings.
     */
    public HttpClient createClient() {
        return new DefaultHttpClient();
    }

    /**
     * Starts the feed archive climbing.
     */
    public void readFeed(URL url) throws IOException {
        initialize();
        try {
            URL followingUrl = url;
            while (followingUrl != null) {
                followingUrl = readFeedPage(followingUrl);
                if (followingUrl != null) {
                    logger.info(".. following: <"+followingUrl+">");
                }
            }
            logger.info("Done.");
        } finally {
            shutdown();
        }
    }

    /**
     * Utility method to use the HttpClient (via {@link createClient}) to open
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
        InputStream inStream = getResponseAsInputStream(url);
        try {
            feed = (Feed) Abdera.getInstance().getParser().parse(
                    inStream, url.toString()).getRoot();

            if (processFeedPage(url, feed)) {
                IRI followingHref = feed.getLinkResolvedHref("prev-archive");
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
     * Template method intended for the feed processing.
     * @return whether to continue backwards in time or stop.
     */
    public abstract boolean processFeedPage(URL pageUrl, Feed feed);

    public static String unescapeColon(String uriPath)
        throws UnsupportedEncodingException {
        // FIXME: we have ":" url-escaped here (via Abdera resolved hrefs).
        // Is this a symptom of a brittle URI strategy in general?
        return uriPath.replace(URLEncoder.encode(":", "utf-8"), ":");
    }

}
