//package ...

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
        // TODO: Read backwards in time until last read date..
        // TODO: Use e.g. depot.startSession(), session.doIndex() ?

        // TODO: storage safety and history:
        // * Verify length and md5.
        // * Store all collected stuff separately(?)
        //   .. or just feeds and pre-rewritten RDF?

        def followingUrl = url
        while (followingUrl) {
            def feed = readFeedPage(followingUrl)
            def followingHref = feed?.getLinkResolvedHref("prev-archive")
            followingUrl = followingHref? new URL(followingHref.toString()) : null
            if (followingUrl)
                logger.info ".. following: <${followingUrl}>"
        }
        logger.info "Done."
    }

    Feed readFeedPage(URL url) {
        logger.info "Reading Feed <${url}> ..."
        def feed
        try {
            feed = Abdera.instance.parser.parse(url.openStream(),
                    url.toString()).root
        } catch (Exception e) {
            logger.exception "Error parsing feed!", e
            return null
        }
        logger.info "Title: ${feed.title}"
        // TODO: Check for tombstones; if so, delete.
        for (entry in feed.entries) {
            storeEntry(entry)
        }
        return feed
    }

    void storeEntry(Entry entry) {
        def entryId = entry.id.toString()
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
            def mimeType = link.mimeType.toString()
            def lang = link.hrefLang
            if (link.rel == "alternate") {
                contents << createDepotContent(urlPath, mimeType, lang)
            }
            if (link.rel == "enclosure") {
                assert urlPath.startsWith(entryId)
                def slug = urlPath.replaceFirst(entryId, "")
                enclosures << createDepotContent(urlPath, mimeType, null, slug)
            }
        }
        logger.info "Saving Entry <${entryId}>"
        println "    " + entryId
        println "    " + timestamp
        println "    " + contents
        println "    " + enclosures
        // FIXME: depot.createEntry(entryId, timestamp, contents, enclosures)
    }

    SourceContent createDepotContent(urlPath, mimeType, lang, slug=null) {
        // TODO: find RDF with suitable mimeType and URIMint new ID (with
        //       rewritten RDF!)
        // FIXME: we have ":" url-escaped here. Is this a symptom of a brittle
        // URI strategy in general?
        urlPath = urlPath.replace(URLEncoder.encode(":", "utf-8"), ":")
        def inStream = new URL(urlPath).openStream()
        return new SourceContent(inStream, slug, mimeType, lang)
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
    }

}
