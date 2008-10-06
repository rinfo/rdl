package se.lagrummet.rinfo.collector


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient

import org.apache.http.HttpVersion
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpParams
import org.apache.http.params.HttpProtocolParams

import org.apache.commons.io.IOUtils

import org.apache.abdera.Abdera
import org.apache.abdera.model.Element
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Source

import org.openrdf.model.vocabulary.XMLSchema
import org.openrdf.repository.Repository

import se.lagrummet.rinfo.store.depot.Atomizer
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.DepotContent
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.DepotEntryBatch
import se.lagrummet.rinfo.store.depot.DuplicateDepotEntryException
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.base.URIMinter

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.atom.FeedArchiveReader

/* TODO: (see details in inline comments)

    - needs error recovery to prevent a state of having read half-way!

    - storage safety and history:
        - Store all collected stuff separately(?)
            .. or just feeds and pre-rewritten RDF?
            - some "extras" support in depot-API for this collector depot?

    - get all feeds until last collected, then read entries forwards in time?
        - and/or just store entries in a "temp session locally",
          and wipe if errors.

    ---- Documentation ----

    * All entries read will have new timestamps based on their actual
      (successful) addition into this depot. This because the resulting feed
      must be ordered by "collect" time (not "jumbled" source times).

*/

class FeedCollector extends FeedArchiveReader {

    private final logger = LoggerFactory.getLogger(FeedCollector)

    public static final String SOURCE_META_FILE_NAME = "collector-source-info.entry"

    FileDepot depot
    URIMinter uriMinter
    Repository statsRepo

    List rdfMimeTypes = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    private DepotEntryBatch collectedBatch

    // TODO:IMPROVE:  Private ctor ok? Synchronized readFeed?
    // Or just warn that this class is not thread safe?

    private FeedCollector(FileDepot depot, Repository statsRepo, URIMinter uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
        this.statsRepo = statsRepo
    }

    public static void readFeed(FileDepot depot, Repository statsRepo,
            URIMinter uriMinter, URL url) {
        new FeedCollector(depot, statsRepo, uriMinter).readFeed(url)
    }

    @Override
    public HttpClient createClient() {
        // TODO: Configure to use SSL (https) and verify cert.!
        // TODO:? httpClient.setHttpRequestRetryHandler(...)

        HttpParams params = new BasicHttpParams()
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
        ConnManagerParams.setMaxTotalConnections(params, 100)

        SchemeRegistry schemeRegistry = new SchemeRegistry()
        schemeRegistry.register(
                new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
        schemeRegistry.register(
                new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))

        ClientConnectionManager clientConnMgr =
                new ThreadSafeClientConnManager(params, schemeRegistry)
        HttpClient httpClient = new DefaultHttpClient(clientConnMgr, params)

        return httpClient
    }

    @Override
    public void initialize() {
        super.initialize()
        this.collectedBatch = depot.makeEntryBatch()
    }

    @Override
    public void shutdown() {
        super.shutdown()
        depot.indexEntries(collectedBatch)
        this.collectedBatch = null
        getClient().getConnectionManager().shutdown()
    }

    @Override
    public URL readFeedPage(URL url) throws IOException {
        // TODO:? never visit pageUrl being an already visited archive page?
        return super.readFeedPage(url)
    }

    public boolean processFeedPage(URL pageUrl, Feed feed) {
        logger.info "Title: ${feed.title}"

        feed = feed.sortEntriesByUpdated(true)
        logVisitedFeedPageStats(feed)
        /* FIXME: revamp for failsafe collecting:

            - store local copy of:
                - feed pages with file url:s
                - entries with intact dates but reminted urls,
              then read that copy forward in time into real depot, with new dates.

            - scroll back in history until last known (via statsRepo)
                - schedule updates and deletes (to avoid reading old entries)

            - IMPORTANT: *then* walk forwards and storeEntry
                - ow. the dates are wrong, and older resources cannot be
                  referenced by newer! And more crucially, a failed read
                  effectively blocks other sources (since it's not yet indexed)!

        */

        // TODO: Check for tombstones; if so, delete in depot.
        def currentUpdated = new Date() // TODO:? fail like this on "future entries" in collect source?
        for (entry in feed.getEntries()) {
            // TODO: verify this assert in unit test instead (using a jumbled source feed)
            assert currentUpdated >= entry.getUpdated()
            currentUpdated = entry.getUpdated()
            try {
                boolean newOrUpdated = storeEntry(feed, entry)
                if (!newOrUpdated) {
                    return false
                }
            } catch (Exception e) {
                // FIXME: rollback and report explicit error!
                throw e
            }
        }
        return true
        /* FIXME: Fail on:
            - retriable:
                java.net.SocketException

            - source errors (needs reporting):
                javax.net.ssl.SSLPeerUnverifiedException
                MissingRdfContentException
                URIComputationException
                DuplicateDepotEntryException
                SourceCheckException (from SourceContent#writeTo), ...

          , then *rollback* (entire batch?) and *report error*!
        */
    }

    protected void logVisitedFeedPageStats(Feed feed) {
        def vf = statsRepo.getValueFactory()
        def conn = statsRepo.getConnection()

        def selfUri = vf.createURI(feed.getSelfLinkResolvedHref().toString())
        def updated = vf.createLiteral(feed.updatedElement.getString(), XMLSchema.DATETIME)
        def updProp = vf.createURI(
                "http://bblfish.net/work/atom-owl/2006-06-06/#", "updated")
        conn.remove(selfUri, updProp, null)
        conn.add(selfUri, updProp, updated)
        // TODO: log isArchive, source, ...?

        conn.close()
    }

    protected boolean storeEntry(Feed sourceFeed, Entry sourceEntry) {

        URI sourceEntryId = sourceEntry.getId().toURI()
        logger.info "Reading Entry <${sourceEntryId}> ..."
        // TODO:? if metaIndex.get(sourceEntryId): if not newOrUpdated: continue..

        List contents = initialContents(sourceEntry)
        List enclosures = new ArrayList()
        fillContentsAndEnclosures(sourceEntry, contents, enclosures)

        URI finalUri = processRdfInPlace(sourceEntryId, contents)

        logger.info("Collecting entry <${sourceEntryId}>  as <${finalUri}>..")

        DepotEntry depotEntry = depot.getEntry(finalUri)
        Date timestamp = new Date()

        if (depotEntry == null) {
            logger.info("New entry <${finalUri}>.")
            depotEntry = depot.createEntry(finalUri, timestamp, contents, enclosures)

        } else {
            /* TODO:
            Is stopping on known entry reliable enough?

            If existing; check stored entry and allow update if *both*
                sourceEntry.updated>.created (above) *and* > depotEntry.updated..
                .. and "source feed" is "same as last"? (indirected via rdf facts)?
            */
            /* FIXME: since we read backwards, a batch with a new then updated will
               force a stop although there may be earlier unvisited entries.
               This should be solved by two-pass collect (as per TODO in
               processFeedPage above).
            .. But "for now", this should do the trick(?):
            */
            if (collectedBatch.contains(depotEntry)) {
                return true;
            }

            if (sourceIsNotAnUpdate(sourceEntry, depotEntry)) {
                logger.info("Encountered collected entry <${sourceEntry.getId()}> at [" +
                        sourceEntry.getUpdated()+"].")
                return false
            }

            // NOTE: If source has been collected but appears as newly published:
            if (!(sourceEntry.getUpdated() > sourceEntry.getPublished())) {
                logger.error("Collected entry <${sourceEntry.getId()} exists as " +
                        " <${finalUri}> but does not appear as updated:" +
                        sourceEntry)
                throw new DuplicateDepotEntryException(depotEntry);
            }

            logger.info("Updating entry <${finalUri}>.")
            depotEntry.update(timestamp, contents, enclosures)
        }

        saveSourceMetaInfo(sourceFeed, sourceEntry, depotEntry)
        //TODO:? metaIndex.indexCollected(sourceId, sourceUpdated, finalUri.toString())

        collectedBatch.add(depotEntry)
        return true
    }

    protected void deleteEntry(Feed sourceFeed, URI entryId, Date deletedDate) {
        // FIXME: implement and use!
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
        URI sourceEntryId = sourceEntry.getId().toURI()
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

    protected SourceContent createSourceContent(urlPath, mediaType, lang, slug=null,
            md5hex=null, length=null) {
        urlPath = unescapeColon(urlPath)
        def srcContent = new CollectingSourceContent(
                this, urlPath, mediaType, lang, slug)
        if (md5hex != null) {
            srcContent.datachecks[SourceContent.Check.MD5] = md5hex
        }
        if (length != null) {
            srcContent.datachecks[SourceContent.Check.LENGTH] = length
        }
        return srcContent
    }


    protected boolean sourceIsNotAnUpdate(Entry sourceEntry, DepotEntry depotEntry) {
        File metaFile = depotEntry.getMetaFile(SOURCE_META_FILE_NAME)
        Entry metaEntry = null
        try {
            metaEntry = (Entry) Abdera.getInstance().getParser().parse(
                    new FileInputStream(metaFile)).getRoot();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Entry <"+depotEntry.getId() +
                    "> is missing expected meta file <"+metaFile+">.")
        }
        // TODO: is this trustworthy enough?
        return !(sourceEntry.getUpdated() > metaEntry.getUpdated())
    }

    protected void saveSourceMetaInfo(Feed sourceFeed, Entry sourceEntry,
            DepotEntry depotEntry) {
        /* TODO:IMPROVE:
            easier to store and use depotEntry.edited = sourceEntry.updated?
            .. but this metadata should be kept, so we can use it
               (reasonably not too much of a performance hit) */
        File metaFile = depotEntry.getMetaFile(SOURCE_META_FILE_NAME)
        // TODO:IMPROVE: only keep id, updated (published)?
        Entry metaEntry = sourceEntry.clone()
        metaEntry.setSource(sourceFeed)
        // TODO:IMPROVE: is this way of setting base URI enough?
        metaEntry.setBaseUri(metaEntry.getSource().getResolvedBaseUri())
        metaEntry.getSource().setBaseUri(null)
        metaEntry.writeTo(new FileOutputStream(metaFile))
    }


    protected URI processRdfInPlace(
            URI sourceEntryId, List<SourceContent> contents) {

        def rdfContent = getRdfContent(sourceEntryId, contents)
        def repo = rdfContentToRepository(rdfContent)

        def newUri = uriMinter.computeOfficialUri(repo) // TODO: give sourceEntryId for subj?
        if (!sourceEntryId.equals(newUri)) {
            logger.info("New URI: <${newUri}>")
            repo = RDFUtil.replaceURI(repo, sourceEntryId, newUri)
        }

        verifyRdf(newUri, repo)

        // TODO:IMPROVE: nicer to keep exact input rdf serialization if not rewritten?
        rdfContent.setSourceStream(RDFUtil.serializeAsInputStream(
                repo, rdfContent.mediaType), true)

        repo.shutDown()
        return newUri
    }

    protected SourceContent getRdfContent(URI sourceEntryId,
            List<SourceContent> contents) {
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
        return rdfContent
    }

    protected boolean isRdfContent(SourceContent content) {
        return rdfMimeTypes.contains(content.mediaType)
    }

    protected Repository rdfContentToRepository(SourceContent rdfContent) {
        def repo = RDFUtil.createMemoryRepository()
        // FIXME: RDFa must be handled more manually (getting, esp. serializing!)
        // TODO:IMPROVE: lots of buffered data going on here; how to streamline?
        // TODO:IMPROVE: "" is baseURI; should be passed (via SourceContent?)?
        // TODO:IMPROVE: keep original RDF as metaFile?
        def rdfOutStream = new ByteArrayOutputStream()
        rdfContent.writeTo(rdfOutStream)
        RDFUtil.loadDataFromStream(repo,
                new ByteArrayInputStream(rdfOutStream.toByteArray()),
                "", rdfContent.mediaType)
        return repo
    }

    protected URI computeNewUri(Repository repo, URI sourceEntryId) {
    }

    protected void verifyRdf(URI docUri, Repository repo) {
        // TODO: use new base.RDFVerifyer#verifyRdf()
    }

}

class CollectingSourceContent extends SourceContent {

    private FeedCollector collector;
    private String urlPath;

    public CollectingSourceContent(FeedCollector collector, String urlPath,
            String mediaType, String lang, String enclosedUriPath) {
        super(mediaType, lang, enclosedUriPath);
        this.collector = collector;
        this.urlPath = urlPath;
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        // TODO:IMPROVE: retrying http; and also handle failed gets in writeTo(File)
        if (getSourceStream() == null) {
            setSourceStream(collector.getResponseAsInputStream(urlPath));
        }
        super.writeTo(outStream);
    }

}
