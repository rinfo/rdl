
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.springframework.context.support.ClassPathXmlApplicationContext as Ctxt

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.DuplicateDepotEntryException
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
    URIMinter uriMinter

    def rdfMimeTypes = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    FeedCollector(depot, uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
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
        //def timestamp = entry.updated
        def timestamp = new Date() // TODO: always just current time?
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
                println "enclosure: ${urlPath}"
                assert urlPath.startsWith(entryId.toString())
                def slug = urlPath.replaceFirst(entryId, "")
                enclosures << createDepotContent(urlPath, mediaType, null, slug)
            }
        }

        def newUri = computeOfficialUriInPlace(entryId, contents)
        logger.info "New URI: <${newUri}>"

        logger.info "Saving Entry <${newUri}>"
        // FIXME: duplicates are hard errors; but *updates* to existing
        // must be possible! How to detect "benign" updates from source? Alts:
        // - require published to be kept; use as is and only diff on updated?
        // - .. <batch:operation type="update">
        // - save orig.url (if different from this, error is in minting)
        // - require publishers to compute URI? (and req. published + updated..)
        try {
            depot.createEntry(newUri, timestamp, contents, enclosures)
        } catch (DuplicateDepotEntryException e) {
            logger.error "Duplicate entry <${newUri}>!"
            logger.warn "Updating anyway."
            def depotEntry = depot.getEntry(newUri)
            depotEntry.update(timestamp, contents, enclosures)
        }
    }

    protected URI computeOfficialUriInPlace(
            URI entryId, List<SourceContent> contents) {
        def repo = RDFUtil.createMemoryRepository()

        // TODO: find RDF with suitable mediaType (don't assume first content!)
        // FIXME: RDFa must be handled more manually (getting, esp. serializing!)
        def content = contents[0]
        // TODO: "" is baseURI; should be passed (via SourceContent?)?
        RDFUtil.loadDataFromStream(repo, content.sourceStream, "", content.mediaType)

        def newUri = uriMinter.computeOfficialUri(repo) // TODO: give entryId for subj?

        def newRepo = RDFUtil.replaceURI(repo, entryId, newUri)

        content.sourceStream = RDFUtil.serializeAsInputStream(
                newRepo, content.mediaType)

        return newUri
    }

    protected SourceContent createDepotContent(urlPath, mediaType, lang, slug=null) {
        urlPath = unescapeColon(urlPath)
        def inStream = new URL(urlPath).openStream()
        return new SourceContent(inStream, mediaType, lang, slug)
    }

    static main(args) {
        if (args.size() != 1) {
            println "Usage: <uri-to-subscription-feed>"
            System.exit 0
        }
        def context = new Ctxt("applicationContext.xml")
        def depot = context.getBean("fileDepot")
        def uriMinter = context.getBean("uriMinter")

        def collector = new FeedCollector(depot, uriMinter)
        collector.readFeed(new URL(args[0]))

        depot.generateIndex()
    }

}
