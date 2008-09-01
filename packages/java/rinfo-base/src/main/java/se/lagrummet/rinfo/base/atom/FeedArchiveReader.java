package se.lagrummet.rinfo.base.atom;

import java.util.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;


public abstract class FeedArchiveReader {

    private final Logger logger = LoggerFactory.getLogger(FeedArchiveReader.class);

    public void readFeed(URL url) throws IOException {
        URL followingUrl = url;
        while (followingUrl != null) {
            followingUrl = readFeedPage(followingUrl);
            if (followingUrl != null) {
                logger.info(".. following: <"+followingUrl+">");
            }
        }
        logger.info("Done.");
    }

    public URL readFeedPage(URL url) throws IOException {
        logger.info("Reading Feed <"+url+"> ...");
        Feed feed;
        URL followingUrl = null;
        ReadableByteChannel inChannel = Channels.newChannel(url.openStream());
        try {
            feed = (Feed) Abdera.getInstance().getParser().parse(inChannel,
                    url.toString()).getRoot();

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
            inChannel.close();
        }
        return followingUrl;
    }

    public static String unescapeColon(String uriPath)
        throws UnsupportedEncodingException
    {
        // FIXME: we have ":" url-escaped here (via Abdera resolved hrefs).
        // Is this a symptom of a brittle URI strategy in general?
        return uriPath.replace(URLEncoder.encode(":", "utf-8"), ":");
    }

    /**
     * @return whether to continue backwards in time or stop.
     */
    public abstract boolean processFeedPage(URL pageUrl, Feed feed);

}
