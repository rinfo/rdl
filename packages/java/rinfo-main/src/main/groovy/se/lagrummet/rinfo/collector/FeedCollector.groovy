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
import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Element
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Source
import org.apache.abdera.i18n.iri.IRI

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
import se.lagrummet.rinfo.base.atom.FeedArchivePastToPresentReader

/* TODO: (see details in inline comments)

    - error recovery tests
    - error reporting

    ---- Documentation ----

    * gets all feeds until last collected, then reads entries forwards in time..

    * uses stateData...

    * All entries read will have new timestamps based on their actual
      (successful) addition into this depot. This because the resulting feed
      must be ordered by "collect" time (not "jumbled" source times).

*/

class FeedCollector extends FeedArchivePastToPresentReader {

    private final logger = LoggerFactory.getLogger(FeedCollector)

    public static final String SOURCE_META_FILE_NAME = "collector-source-info.entry"

    FileDepot depot
    URIMinter uriMinter
    FeedCollectorStateData stateData

    List rdfMimeTypes = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    private DepotEntryBatch collectedBatch

    // TODO:IMPROVE: Not thread safe - is private ctor ok? Synchronized readFeed?

    private FeedCollector(FileDepot depot, Repository stateRepo, URIMinter uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
        this.stateData = new FeedCollectorStateData(stateRepo)
    }

    public static void readFeed(FileDepot depot, Repository stateRepo,
            URIMinter uriMinter, URL url) {
        new FeedCollector(depot, stateRepo, uriMinter).readFeed(url)
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
    public void shutdown() {
        try {
            super.shutdown()
        } finally {
            getClient().getConnectionManager().shutdown()
            this.stateData.shutdown()
        }
    }


    @Override
    boolean stopOnEntry(Entry entry) {
        return stateData.hasCollected(entry)
    }

    @Override
    boolean hasVisitedArchivePage(URL pageUrl) {
        // TODO: never visit pageUrl being an already visited archive page?
        return false
    }

    @Override
    public void processFeedPageInOrder(URL pageUrl, Feed feed) {
        logger.info("Processing feed page: <${pageUrl}> (id <${feed.id}>)")

        stateData.logVisitedFeedPage(feed)
        collectedBatch = depot.makeEntryBatch()
        def deletedMap = depot.getAtomizer().getDeletedMarkers(feed)
        def currentUpdated
        try {
            for (entry in feed.getEntries()) {
                if (deletedMap.containsKey(entry.getId())) {
                    // TODO:? this skips any preceding updates of deleteds in page..
                    continue
                }
                if (stateData.hasCollected(entry)) {
                    if (logger.isDebugEnabled())
                        logger.debug "skipping collected entry <${entry.id}> [${entry.updated}]"
                    continue
                }
                // TODO:? verify this in unit test instead (using a jumbled source feed)
                if (currentUpdated != null) {
                    assert currentUpdated <= entry.getUpdated()
                }
                currentUpdated = entry.getUpdated()
                try {
                    storeEntry(feed, entry)
                } catch (Exception e) {
                    // TODO: report explicit error.. storeEntry should catch most though..
                    logger.error("Error storing entry!", e)
                    throw e
                }
            }
            deleteFromMarkers(feed, deletedMap)
        } finally {
            depot.indexEntries(collectedBatch)
            collectedBatch = null
        }
    }

    protected void deleteFromMarkers(Feed sourceFeed, Map<IRI, AtomDate> deletedMap) {
        for (Map.Entry<URI, Date> delItem : deletedMap.entrySet()) {
            try {
                deleteEntry(sourceFeed,
                        delItem.getKey().toURI(),
                        delItem.getValue().getDate())
            } catch (Exception e) {
                // TODO: report explicit error.. storeEntry should catch most though..
                logger.error("Error deleting entry!", e)
                throw e
            }
        }
    }

    protected void storeEntry(Feed sourceFeed, Entry sourceEntry) {
        URI sourceEntryId = sourceEntry.getId().toURI()
        logger.info "Reading Entry <${sourceEntryId}> ..."

        List contents = initialContents(sourceEntry)
        List enclosures = new ArrayList()
        fillContentsAndEnclosures(sourceEntry, contents, enclosures)

        URI finalUri = processRdfInPlace(sourceEntryId, contents)

        logger.info("Collecting entry <${sourceEntryId}>  as <${finalUri}>..")
        DepotEntry depotEntry = depot.getEntry(finalUri)
        Date timestamp = new Date()

        try {
            if (depotEntry == null) {
                logger.info("New entry <${finalUri}>.")
                depotEntry = depot.createEntry(
                        finalUri, timestamp, contents, enclosures)
            } else {
                /* TODO:IMPROVE:?
                If existing; check stored entry and allow update if *both*
                    sourceEntry.updated>.created (above) *and* > depotEntry.updated..
                    .. and "source feed" is "same as last"? (indirected via rdf facts)?
                */
                if (sourceIsNotAnUpdate(sourceEntry, depotEntry)) {
                    logger.info("Encountered collected entry <" +
                            sourceEntry.getId()+"> at [" +
                            sourceEntry.getUpdated()+"].")
                    return
                }
                // NOTE: If source has been collected but appears as newly published:
                if (!(sourceEntry.getUpdated() > sourceEntry.getPublished())) {
                    logger.error("Collected entry <"+sourceEntry.getId() +
                            " exists as <"+finalUri +
                            "> but does not appear as updated:" +
                            sourceEntry)
                    throw new DuplicateDepotEntryException(depotEntry);
                }
                logger.info("Updating entry <${finalUri}>.")
                depotEntry.update(timestamp, contents, enclosures)
            }
            saveSourceMetaInfo(sourceFeed, sourceEntry, depotEntry)
            stateData.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)
            collectedBatch.add(depotEntry)
        } catch (Exception e) {
            depotEntry.rollback()
            return
            /* FIXME: handle errors:
                - retriable:
                    java.net.SocketException

                - source errors (needs reporting):
                    javax.net.ssl.SSLPeerUnverifiedException
                    MissingRdfContentException
                    URIComputationException
                    DuplicateDepotEntryException
                    SourceCheckException (from SourceContent#writeTo), ...

               Index the ok ones, *rollback* last depotEntry and *report error*!
            */
        }
    }

    protected void deleteEntry(Feed sourceFeed, URI sourceEntryId, Date deletedDate) {
        // FIXME: saveSourceMetaInfo (which may be present)
        def entryId = stateData.getDepotIdBySourceId(sourceEntryId)
        // TODO: this being null means we have lost collector metadata!
        DepotEntry depotEntry = depot.getEntry(entryId)
        logger.info("Deleting entry <${entryId}>.")
        depotEntry.delete(deletedDate)
        stateData.logUpdatedEntry(sourceFeed, sourceEntryId, deletedDate, depotEntry)
        collectedBatch.add(depotEntry)
        //TODO:..stateData.logDeletedEntry
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
