package se.lagrummet.rinfo.main


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

import org.apache.abdera.Abdera
import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Link
import org.apache.abdera.i18n.iri.IRI

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.store.depot.Atomizer
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.DepotEntryBatch
import se.lagrummet.rinfo.store.depot.DuplicateDepotEntryException
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.URIComputationException
import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.collector.atom.FeedArchivePastToPresentReader

/* TODO: (see details in inline comments)

    - error recovery tests
    - error reporting

    ---- Documentation ----

    * gets all feeds until last collected, then reads entries forwards in time.

    * uses registry...

    * All entries read will have new timestamps based on their actual
      (successful) addition into this depot. This because the resulting feed
      must be ordered by "collect" time (not source times, since multiple
      sources have no interdependent ordering).

*/
class FeedCollector extends FeedArchivePastToPresentReader {

    private final Logger logger = LoggerFactory.getLogger(FeedCollector)

    public static final String SOURCE_META_FILE_NAME = "collector-source-info.entry"

    FileDepot depot
    URIMinter uriMinter
    FeedCollectorRegistry registry

    List rdfMimeTypes = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    private DepotEntryBatch collectedBatch

    /**
     * Important: this class is not thread safe. Create a new instance for each
     * collect session.
     */
    private FeedCollector(Storage storage) {
        this.depot = storage.getDepot()
        this.registry = storage.newFeedCollectorRegistry()
        this.uriMinter = storage.uriMinter
    }

    public static void readFeed(Storage storage, URL url) {
        def collector = new FeedCollector(storage)
        collector.readFeed(url)
        collector.shutdown()
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
            try {
                this.registry.shutdown()
            } finally {
                getClient().getConnectionManager().shutdown()
            }
        }
    }


    @Override
    boolean stopOnEntry(Entry entry) {
        return registry.hasCollected(entry)
    }

    @Override
    boolean hasVisitedArchivePage(URL pageUrl) {
        // TODO: never visit pageUrl being an already visited archive page?
        return false
    }

    @Override
    public void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deletedMap) {
        logger.info("Processing feed page: <${pageUrl}> (id <${feed.id}>)")

        registry.logVisitedFeedPage(pageUrl, feed)
        collectedBatch = depot.makeEntryBatch()
        try {
            for (entry in effectiveEntries) {
                // TODO: isn't this a strange exceptional state now?
                // FeedArchivePastToPresentReader should never encounter known stuff..
                if (registry.hasCollected(entry)) {
                    if (logger.isDebugEnabled())
                        logger.debug "skipping collected entry <${entry.id}> [${entry.updated}]"
                    continue
                }
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
        for (Map.Entry<IRI, AtomDate> delItem : deletedMap.entrySet()) {
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

        List contents = new ArrayList()
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
            registry.logUpdatedEntry(sourceFeed, sourceEntry, depotEntry)
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
        def entryId = registry.getDepotIdBySourceId(sourceEntryId)
        // TODO: this being null means we have lost collector metadata!
        DepotEntry depotEntry = depot.getEntry(entryId)
        logger.info("Deleting entry <${entryId}>.")
        depotEntry.delete(deletedDate)
        registry.logUpdatedEntry(sourceFeed, sourceEntryId, deletedDate, depotEntry)
        collectedBatch.add(depotEntry)
        //TODO:..registry.logDeletedEntry
    }

    protected void fillContentsAndEnclosures(Entry sourceEntry,
            List contents, List enclosures) {
        contents.add(getMainContent(sourceEntry))
        for (link in sourceEntry.links) {
            def urlPath = link.resolvedHref.toString()
            def mediaType = link.mimeType.toString()
            def lang = link.hrefLang
            def len = link.getLength()
            def md5hex = link.getAttributeValue(Atomizer.LINK_EXT_MD5)
            if (link.rel == "alternate") {
                contents.add(createSourceContent(
                        urlPath, mediaType, lang, null, md5hex, len))
            } else if (link.rel == "enclosure") {
                def slug = getEnclosureSlug(
                        sourceEntry.getId().toURI(), link.resolvedHref.toURI())
                enclosures.add(createSourceContent(
                        urlPath, mediaType, null, slug, md5hex, len))
            }
        }
    }

    protected RemoteSourceContent getMainContent(Entry sourceEntry) {
        def contentElem = sourceEntry.getContentElement()
        def contentUrlPath = contentElem.getResolvedSrc().toString()
        def contentMimeType = contentElem.getMimeType().toString()
        def contentLang = contentElem.getLanguage()
        def contentMd5hex = contentElem.getAttributeValue(Atomizer.LINK_EXT_MD5)
        // TODO: allow inline content!
        return createSourceContent(
                contentUrlPath, contentMimeType, contentLang, null, contentMd5hex)
    }

    protected RemoteSourceContent createSourceContent(urlPath,
            mediaType, lang, slug=null,
            md5hex=null, length=null) {
        urlPath = unescapeColon(urlPath)
        def srcContent = new RemoteSourceContent(
                this, urlPath, mediaType, lang, slug)
        if (md5hex != null) {
            srcContent.datachecks[SourceContent.Check.MD5] = md5hex
        }
        if (length != null) {
            srcContent.datachecks[SourceContent.Check.LENGTH] = length
        }
        return srcContent
    }


    protected static String getEnclosureSlug(URI sourceEntryUri, URI enclosureUri) {
        String entryIdBase = sourceEntryUri.getPath()
        String enclPath = enclosureUri.getPath()
        if (!enclPath.startsWith(entryIdBase)) {
            // TODO: fail with what?
            throw new RuntimeException("Entry <"+sourceEntryUri +
                    "> references <${enclosureUri}> out of its domain.")
        }
        return enclPath
    }


    protected static boolean sourceIsNotAnUpdate(Entry sourceEntry,
            DepotEntry depotEntry) {
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

    protected static void saveSourceMetaInfo(Feed sourceFeed, Entry sourceEntry,
            DepotEntry depotEntry) {
        File metaFile = depotEntry.getMetaFile(SOURCE_META_FILE_NAME)
        // TODO:IMPROVE: only keep id, updated (published)?
        Entry metaEntry = sourceEntry.clone()
        metaEntry.setSource(sourceFeed)
        // TODO:IMPROVE: is this way of setting base URI enough?
        metaEntry.setBaseUri(metaEntry.getSource().getResolvedBaseUri())
        metaEntry.getSource().setBaseUri(null)
        metaEntry.writeTo(new FileOutputStream(metaFile))
    }


    // TODO: factor out rdf-churning parts.

    protected URI processRdfInPlace(
            URI sourceEntryId, List<SourceContent> contents) {

        def rdfContent = getRdfContent(sourceEntryId, contents)
        def repo = rdfContentToRepository(rdfContent)

        // TODO: switch handling based on RDF.TYPE?
        def canonicalUri = sourceEntryId
        try {
            def newUri = uriMinter.computeOfficialUri(repo) // TODO: give sourceEntryId for subj?
            if (newUri && !newUri.equals(sourceEntryId)) {
                logger.info("New URI: <${newUri}>")
                repo = RDFUtil.replaceURI(repo, sourceEntryId, newUri)
                canonicalUri = newUri
            }
        } catch (URIComputationException e) {
            // FIXME: establish rules for which resources to compute URI:s for!
        }

        verifyRdf(canonicalUri, repo)

        // TODO:IMPROVE: nicer to keep exact input rdf serialization if not rewritten?
        rdfContent.setSourceStream(RDFUtil.serializeAsInputStream(
                repo, rdfContent.mediaType), true)

        repo.shutDown()
        return canonicalUri
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

    protected void verifyRdf(URI docUri, Repository repo) {
        // TODO: use new base.RDFVerifyer#verifyRdf()
    }

}

class RemoteSourceContent extends SourceContent {

    private FeedCollector collector;
    String urlPath;

    public RemoteSourceContent(FeedCollector collector, String urlPath,
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
