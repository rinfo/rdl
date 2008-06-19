
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.springframework.context.support.ClassPathXmlApplicationContext as Ctxt

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.base.URIMinter

import se.lagrummet.rinfo.util.rdf.RDFUtil
import se.lagrummet.rinfo.util.atom.FeedArchiveReader

/* TODO:

    - Read backwards in time until last read date..
    - Use e.g. collector.startSession(), session.doIndex() ?
    - we also need error recovery (rollback? store "atDate"?)
        to prevent a state of having read half-way!
    - perhaps get all feeds until minDateTime, then read entries
        forwards in time?
        - and/or just store entries in a "temp session locally",
        and wipe if errors.
    .. Reasonably; all entries read will have new timestamps
        based on their actual (successful) addition into this depot.

    - storage safety and history:
        - Verify length and md5.
        - Store all collected stuff separately(?)
            .. or just feeds and pre-rewritten RDF?

*/

class FeedCollector extends FeedArchiveReader {

    private final logger = LoggerFactory.getLogger(FeedCollector)

    FileDepot depot

    def rdfMimeTypes = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    FeedCollector(depot) {
        this.depot = depot
    }

    boolean processFeedPage(URL pageUrl, Feed feed) {
        logger.info "Title: ${feed.title}"
        // TODO: Check for tombstones; if so, delete in depot.
        for (entry in feed.entries) {
            storeEntry(entry)
        }
        // TODO: stop at feed with entry at minDateTime..
        //Date minDateTime=null
        return true
    }

    void storeEntry(Entry entry) {

        def entryId = entry.id.toURI()
        //def timestamp = new Date() // FIXME: depot doesn't yet handle lots in same day
        def timestamp = entry.updated
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

        // TODO: find RDF with suitable mediaType (don't assume first content!)
        // TODO: and *don't read URL twice! .. more support from SourceContent?
        //def repo = RDFUtil.createMemoryRepository()
        //RDFUtil.loadDataFromURL(repo, new URL(contentUrlPath), contentMimeType)
        //def newUri = URIMinter.computeOfficialUri(repo, entryId)
        // TODO: replace resource with serialized newRepo (rewritten RDF)
        //def newRepo = RDFUtil.replaceURI(entryId, newUri)
        //def doc = RDFUtil.serialize(newRepo, format)
        // ...

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
        collector.readFeed(new URL(args[0]))
        fileDepot.generateIndex()
    }

}
