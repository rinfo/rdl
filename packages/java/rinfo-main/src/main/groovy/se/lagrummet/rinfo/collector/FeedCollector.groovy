package se.lagrummet.rinfo.collector


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.DuplicateDepotEntryException
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.base.URIMinter

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.atom.FeedArchiveReader

/* TODO:

    - Read backwards in time until last read date (last known youngest per feed?)..
    - Use e.g. collector.startSession(), session.doIndex() ?

    - we also need error recovery (rollback? store "atDate"?)
        to prevent a state of having read half-way!
    - perhaps get all feeds until minDateTime, then read entries
        forwards in time?
        - and/or just store entries in a "temp session locally",
        and wipe if errors.


    .. Reasonably; all entries read will have new timestamps
        based on their actual (successful) addition into this depot.

    - add "extras" support in depot-API to store collector state (lastRead)?
        .. no, write in an own dir..
        .. but perhaps at least for entries? for source docs? but.. hm..
        .. Begin with own dir!

    - storage safety and history:
        - Verify length and md5.
        - Store all collected stuff separately(?)
            .. or just feeds and pre-rewritten RDF?
                .. support "extras" in depot-API? (See above)
        - verify source certificates

*/

class FeedCollector extends FeedArchiveReader {

    private final logger = LoggerFactory.getLogger(FeedCollector)

    FileDepot depot
    URIMinter uriMinter

    List rdfMimeTypes = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    private Collection<DepotEntry> collectedBatch

    FeedCollector(FileDepot depot, URIMinter uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
    }

    public static void readFeed(FileDepot depot, URIMinter uriMinter, URL url) {
        new FeedCollector(depot, uriMinter).readFeed(url)
    }

    // TODO: synchronized? Or just warn - not thread safe?
    public void readFeed(URL url) throws IOException {
        this.collectedBatch = depot.makeEntryBatch()
        super.readFeed(url)
        depot.indexEntries(collectedBatch)
        this.collectedBatch = null
    }

    boolean processFeedPage(URL pageUrl, Feed feed) {
        logger.info "Title: ${feed.title}"
        // TODO: Check for tombstones; if so, delete in depot.
        for (entry in feed.entries) {
            try {
                storeEntry(entry)
            } catch (DuplicateDepotEntryException e) {
                // FIXME: this isn't reliable; should use explicit "stop at dateTime"
                return false
            }
        }
        // TODO: stop at feed with entry at minDateTime..
        //Date minDateTime=null
        return true
    }

    void storeEntry(Entry sourceEntry) {

        def entryId = sourceEntry.id.toURI()
        // TODO: always just current time? Yes, exposed feed must be with "live" time..
        //  - for one, since we collect multiple feeds, and must not fill feed with
        //  - non-chronological dates!
        //  - so don't use:: def timestamp = sourceEntry.updated
        def timestamp = new Date()
        def contents = []
        def enclosures = []

        logger.info "Reading Entry <${entryId}> ..."

        def contentElem = sourceEntry.contentElement
        def contentUrlPath = contentElem.resolvedSrc.toString()
        def contentMimeType = contentElem.mimeType.toString()
        def contentLang = contentElem.language

        contents << createDepotContent(
                contentUrlPath, contentMimeType, contentLang)

        for (link in sourceEntry.links) {
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

        logger.info "Collecting entry <${sourceEntry.getId()}>  as <${newUri}>.."

        def depotEntry = depot.getEntry(newUri)

        if (depotEntry == null) {
            logger.info "New entry <${newUri}>."
            depotEntry = depot.createEntry(newUri, timestamp, contents, enclosures)

        } else {

            // TODO: Will read same updated entry twice - must stop at last
            //       datetime, see above).
            if (!(sourceEntry.getUpdated() > sourceEntry.getPublished())) {
                /* TODO:
                If existing; check stored entry and allow update if *both*
                 sourceEntry.updated>.created (above) *and* > depotEntry.updated..
                 .. and "source" is "same as last" (indirected via rdf facts)?
                But we cannot reliably do this comparison (since depot creates
                its own update date)::
                    entry.getUpdated() <= depotEntry.getUpdated()
                */
                logger.error("Collected entry <${sourceEntry.getId()} exists as " +
                        " <${newUri}> but does not appear as updated:" +
                        sourceEntry)
                throw new DuplicateDepotEntryException(depotEntry);
            }

            logger.info "Updating entry <${newUri}>."
            depotEntry.update(timestamp, contents, enclosures)
        }

        collectedBatch.add(depotEntry)

    }

    protected boolean isRdfContent(SourceContent content) {
        return rdfMimeTypes.contains(content.mediaType)
    }

    protected URI computeOfficialUriInPlace(
            URI entryId, List<SourceContent> contents) {
        def repo = RDFUtil.createMemoryRepository()

        def rdfContent = null
        for (SourceContent content : contents) {
            if (isRdfContent(content)) {
                rdfContent = content
                break
            }
        }
        if (rdfContent == null) {
            throw new MissingRdfContentException("Found no RDF in <${entryId}>.")
        }
        // FIXME: RDFa must be handled more manually (getting, esp. serializing!)

        // TODO: "" is baseURI; should be passed (via SourceContent?)?
        RDFUtil.loadDataFromStream(repo, rdfContent.sourceStream, "", rdfContent.mediaType)

        def newUri = uriMinter.computeOfficialUri(repo) // TODO: give entryId for subj?

        /* TODO:
        if (oldUri.equals(newUri)) {
            return newUri;
        }
        */
        def newRepo = RDFUtil.replaceURI(repo, entryId, newUri)

        rdfContent.sourceStream = RDFUtil.serializeAsInputStream(
                newRepo, rdfContent.mediaType)

        return newUri
    }

    protected SourceContent createDepotContent(urlPath, mediaType, lang, slug=null) {
        urlPath = unescapeColon(urlPath)
        def inStream = new URL(urlPath).openStream()
        return new SourceContent(inStream, mediaType, lang, slug)
    }

}
