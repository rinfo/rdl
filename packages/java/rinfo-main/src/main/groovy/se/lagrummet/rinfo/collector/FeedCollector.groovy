package se.lagrummet.rinfo.collector


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Element

import se.lagrummet.rinfo.store.depot.Atomizer
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.DepotContent
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


    - add "extras" support in depot-API to store collector state (lastRead)?
        .. no, write in an own dir..
        .. but perhaps at least for entries? for source docs? but.. hm..
        .. Begin with own dir!

    - storage safety and history:
        - Store all collected stuff separately(?)
            .. or just feeds and pre-rewritten RDF?
                .. support "extras" in depot-API? (See above)
        - verify source certificates

    ---- Documentation ----

    * All entries read will have new timestamps based on their actual
      (successful) addition into this depot. This because the resulting feed
      must be ordered by "collect" time (not "jumbled" source times).

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

    // TODO:IMPROVE: synchronized? Or just warn - not thread safe?
    public void readFeed(URL url) throws IOException {
        this.collectedBatch = depot.makeEntryBatch()
        super.readFeed(url)
        depot.indexEntries(collectedBatch)
        this.collectedBatch = null
    }

    public boolean processFeedPage(URL pageUrl, Feed feed) {
        logger.info "Title: ${feed.title}"
        // TODO:? never visit pageUrl being an already visited archive page?
        // TODO: Check for tombstones; if so, delete in depot.
        boolean continueCollect = true
        for (entry in feed.entries) {
            try {
                boolean newOrUpdated = storeEntry(entry)
                if (!newOrUpdated) {
                    continueCollect = false
                }
            } catch (DuplicateDepotEntryException e) {
                // TODO: don't catch when sourceIsNotAnUpdate is implemented (see below)
                return false
            } catch (Exception e) {
                // FIXME: rollback and report explicit error!
                throw e
            }
        }
        return continueCollect
    }

    protected boolean storeEntry(Entry sourceEntry) {
        /* TODO:
        Fail on MissingRdfContentException, URIComputationException,
                DuplicateDepotEntryException,
                SourceCheckException (from SourceContent#writeTo), ...
            - then *rollback* (entire batch?)!

        throw new Exception("Bad MD5 checksum in " +
                depotEntry.getId()+" for "+mapEntry.getKey()+". Was "+
                mapEntry.getValue()+", expected "+storedMd5Hex+".")
        */

        URI sourceEntryId = sourceEntry.getId().toURI()
        logger.info "Reading Entry <${sourceEntryId}> ..."
        // TODO:? if metaIndex.get(sourceEntryId): if not newOrUpdated: continue..

        List contents = initialContents(sourceEntry)
        List enclosures = new ArrayList()
        fillContentsAndEnclosures(sourceEntry, contents, enclosures)

        URI newUri = computeOfficialUriInPlace(sourceEntryId, contents)
        logger.info("Collecting entry <${sourceEntryId}>  as <${newUri}>..")

        DepotEntry depotEntry = depot.getEntry(newUri)
        Date timestamp = new Date()

        if (depotEntry == null) {
            logger.info("New entry <${newUri}>.")
            depotEntry = depot.createEntry(newUri, timestamp, contents, enclosures)

        } else {
            /* TODO:
            Will read same updated entry twice - must stop if known entry
            (is that reliable enough?)

            If existing; check stored entry and allow update if *both*
                sourceEntry.updated>.created (above) *and* > depotEntry.updated..
                .. and "source" is "same as last" (indirected via rdf facts)?
            But we cannot reliably do this comparison (since depot creates
            its own update date)::
                entry.getUpdated() <= depotEntry.getUpdated()
            */
            if (sourceIsNotAnUpdate(sourceEntry, depotEntry)) {
                return false
            }

            // NOTE: If source has been collected but appears as newly published:
            if (!(sourceEntry.getUpdated() > sourceEntry.getPublished())) {
                logger.error("Collected entry <${sourceEntry.getId()} exists as " +
                        " <${newUri}> but does not appear as updated:" +
                        sourceEntry)
                throw new DuplicateDepotEntryException(depotEntry);
            }

            logger.info("Updating entry <${newUri}>.")
            depotEntry.update(timestamp, contents, enclosures)
        }

        saveSourceMetaInfo(sourceEntry, depotEntry)
        //TODO:? metaIndex.indexCollected(sourceId, sourceUpdated, newUri.toString())

        collectedBatch.add(depotEntry)
        return true
    }

    protected List initialContents(Entry sourceEntry) {
        def contentElem = sourceEntry.getContentElement()
        def contentUrlPath = contentElem.getResolvedSrc().toString()
        def contentMimeType = contentElem.getMimeType().toString()
        def contentLang = contentElem.getLanguage()
        List contents = new ArrayList()
        def contentMd5hex = contentElem.getAttributeValue(Atomizer.LINK_EXT_MD5)
        // TODO: allow inline content!
        contents.add(createSourceContent(
                contentUrlPath, contentMimeType, contentLang, null, contentMd5hex))
        return contents
    }

    protected void fillContentsAndEnclosures(Entry sourceEntry,
            List contents, List enclosures) {
        for (link in sourceEntry.links) {
            def urlPath = link.resolvedHref.toString()
            def mediaType = link.mimeType.toString()
            def lang = link.hrefLang
            def len = link.getLength()
            def md5hex = link.getAttributeValue(Atomizer.LINK_EXT_MD5)
            if (link.rel == "alternate") {
                contents.add(createSourceContent(
                        urlPath, mediaType, lang, null, md5hex, len))
            }
            if (link.rel == "enclosure") {
                if (!urlPath.startsWith(sourceEntryId.toString())) {
                    // TODO: fail with what?
                    throw new RuntimeException("Entry <"+sourceEntryId +
                            "> references <${urlPath}> out of its domain.")
                }
                def slug = urlPath.replaceFirst(sourceEntryId, "")
                enclosures.add(createSourceContent(
                        urlPath, mediaType, null, slug, md5hex, len))
            }
        }
    }

    protected boolean isRdfContent(SourceContent content) {
        return rdfMimeTypes.contains(content.mediaType)
    }

    protected URI computeOfficialUriInPlace(
            URI sourceEntryId, List<SourceContent> contents) {
        def repo = RDFUtil.createMemoryRepository()

        def rdfContent = null
        for (SourceContent content : contents) {
            if (isRdfContent(content)) {
                rdfContent = content
                break
            }
        }
        if (rdfContent == null) {
            throw new MissingRdfContentException("Found no RDF in <${sourceEntryId}>.")
        }
        // FIXME: RDFa must be handled more manually (getting, esp. serializing!)

        // TODO:IMPROVE: "" is baseURI; should be passed (via SourceContent?)?

        // TODO:IMPROVE: lots of buffered data going on here; how to streamline?
        def checkedOutStream = new ByteArrayOutputStream()
        rdfContent.writeTo(checkedOutStream)
        RDFUtil.loadDataFromStream(repo,
                new ByteArrayInputStream(checkedOutStream.toByteArray()),
                "", rdfContent.mediaType)

        def newUri = uriMinter.computeOfficialUri(repo) // TODO: give sourceEntryId for subj?

        if (!sourceEntryId.equals(newUri)) {
            logger.info("New URI: <${newUri}>")
            repo = RDFUtil.replaceURI(repo, sourceEntryId, newUri)
        }
        // TODO:IMPROVE: nicer to keep exact input rdf serialization if not rewritten?
        rdfContent.setSourceStream(RDFUtil.serializeAsInputStream(
                repo, rdfContent.mediaType))
        rdfContent.datachecks.clear()

        return newUri
    }

    protected SourceContent createSourceContent(urlPath, mediaType, lang, slug=null,
            md5hex=null, length=null) {
        urlPath = unescapeColon(urlPath)
        // TODO: inStream via https; verify cert.!
        def inStream = new URL(urlPath).openStream()
        def srcContent = new SourceContent(inStream, mediaType, lang, slug)
        if (md5hex != null) {
            srcContent.datachecks[SourceContent.Check.MD5] = md5hex
        }
        if (length != null) {
            srcContent.datachecks[SourceContent.Check.LENGTH] = length
        }
        return srcContent
    }

    protected boolean sourceIsNotAnUpdate(Entry sourceEntry, DepotEntry depotEntry) {
        // FIXME: implement!
        File metaFile = depotEntry.getMetaFile(SOURCE_META_FILE_NAME)
        /*
        if (!metaFile.isFile()) {
            throw new IllegalStateException("Entry <"+depotEntry.getId() +
                    "> is missing expected meta file <"+metaFile+">.")
        }
        */
        return false
    }

    protected void saveSourceMetaInfo(Entry sourceEntry, DepotEntry depotEntry) {
        // FIXME: implement!
        File metaFile = depotEntry.getMetaFile(SOURCE_META_FILE_NAME)
        // TODO: save id, updated (published), source (id, updated, link/@rel=self)
    }

    public static final String SOURCE_META_FILE_NAME = "collector-source-info.entry"

}
