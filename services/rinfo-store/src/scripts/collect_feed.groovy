//package ...

import java.nio.channels.Channels

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.springframework.context.support.ClassPathXmlApplicationContext as Ctxt

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.SourceContent


class FeedCollector {

    private final logger = LoggerFactory.getLogger(FeedCollector)

    FileDepot depot

    def rdfMimeTypes = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    FeedCollector(depot) {
        this.depot = depot
    }

    void readFeed(URL url, Date haltAt=null) {
        /* TODO:
            - Read backwards in time until last read date..
            - Use e.g. depot.startSession(), session.doIndex() ?
            - we also need error recovery (rollback? store "atDate"?)
              to prevent a state of having read half-way!
            - perhaps get all feeds until haltAt, then read entries
              forwards in time?
        */
        /* TODO: storage safety and history:
            - Verify length and md5.
            - Store all collected stuff separately(?)
              .. or just feeds and pre-rewritten RDF?
        */

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
        def followingUrl
        def inChannel = Channels.newChannel(url.openStream())
        try {
            feed = Abdera.instance.parser.parse(inChannel,
                    url.toString()).root
            logger.info "Title: ${feed.title}"
            // TODO: Check for tombstones; if so, delete.
            for (entry in feed.entries) {
                storeEntry(entry)
            }
            def followingHref = feed.getLinkResolvedHref("prev-archive")
            followingUrl = followingHref? followingHref.toURL() : null
        } catch (Exception e) {
            logger.exception "Error parsing feed!", e
            followingUrl = null
        } finally {
            inChannel.close()
        }
        return followingUrl
    }

    void storeEntry(Entry entry) {
        /* TODO:
            - new entryId from URIMinter
            - and find RDF with suitable mediaType and URIMint new ID (with
              rewritten RDF!)
        */

        def entryId = entry.id.toURI()
        def timestamp= new Date()
        def contents = []
        def enclosures = []

        logger.info "Reading Entry <${entryId}> ..."

        def contentElem = entry.contentElement
        def contentUrlPath = contentElem.resolvedSrc.toString()
        def contentMimeType = contentElem.mimeType.toString()
        def contentLang = contentElem.language
        contents << createDepotContent(
                contentUrlPath, contentMimeType, contentLang)

        for (link in entry.links) {
            def urlPath = link.resolvedHref.toString()
            def mediaType = link.mimeType.toString()
            def lang = link.hrefLang
            if (link.rel == "alternate") {
                contents << createDepotContent(urlPath, mediaType, lang)
            }
            if (link.rel == "enclosure") {
                assert urlPath.startsWith(entryId)
                def slug = urlPath.replaceFirst(entryId, "")
                enclosures << createDepotContent(urlPath, mediaType, null, slug)
            }
        }
        logger.info "Saving Entry <${entryId}>"
        depot.createEntry(entryId, timestamp, contents, enclosures)
    }

    SourceContent createDepotContent(urlPath, mediaType, lang, slug=null) {
        // FIXME: we have ":" url-escaped here. Is this a symptom of a brittle
        // URI strategy in general?
        urlPath = urlPath.replace(URLEncoder.encode(":", "utf-8"), ":")
        def inStream = new URL(urlPath).openStream()
        return new SourceContent(inStream, mediaType, lang, slug)
    }

    static main(args) {
        if (args.size() != 1) {
            println "Usage: <uri-to-subscription-feed>"
            System.exit 0
        }
        def context = new Ctxt("applicationContext.xml")
        def fileDepot = context.getBean("fileDepot")
        def collector = new FeedCollector(fileDepot)
        collector.readFeed new URL(args[0])
        fileDepot.generateIndex()
    }

}
