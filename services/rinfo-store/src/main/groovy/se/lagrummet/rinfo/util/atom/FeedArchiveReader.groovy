package se.lagrummet.rinfo.util.atom

import java.nio.channels.Channels

import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry


abstract class FeedArchiveReader {

    private final logger = LoggerFactory.getLogger(FeedArchiveReader)

    void readFeed(URL url) {
        def followingUrl = url
        while (followingUrl) {
            followingUrl = readFeedPage(followingUrl)
            if (followingUrl)
                logger.info ".. following: <${followingUrl}>"
        }
        logger.info "Done."
    }

    URL readFeedPage(URL url) {
        logger.info "Reading Feed <${url}> ..."
        def feed
        def followingUrl = null
        def inChannel = Channels.newChannel(url.openStream())
        try {
            feed = Abdera.instance.parser.parse(inChannel,
                    url.toString()).root

            if (processFeedPage(url, feed)) {
                def followingHref = feed.getLinkResolvedHref("prev-archive")
                followingUrl = followingHref? followingHref.toURL() : null
            }

        } catch (Exception e) {
            logger.error "Error parsing feed!", e
            throw e // TODO: stop on error?
            followingUrl = null
        } finally {
            inChannel.close()
        }
        return followingUrl
    }

    static String unescapeColon(String uriPath) {
        // FIXME: we have ":" url-escaped here (via Abdera resolved hrefs).
        // Is this a symptom of a brittle URI strategy in general?
        return uriPath.replace(URLEncoder.encode(":", "utf-8"), ":")
    }

    /**
     * @return whether to continue backwards in time or stop.
     */
    abstract boolean processFeedPage(URL pageUrl, Feed feed);

}
