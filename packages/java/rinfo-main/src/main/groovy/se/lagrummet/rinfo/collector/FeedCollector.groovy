package se.lagrummet.rinfo.collector


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient

import org.apache.http.HttpVersion
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.conn.params.ConnPerRouteBean
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

    public static final String SOURCE_META_FILE_NAME = "collector-source-info.entry"

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

    public URL readFeedPage(URL url) throws IOException {
        // TODO:? never visit pageUrl being an already visited archive page?
        return super.readFeedPage(url)
    }

    public boolean processFeedPage(URL pageUrl, Feed feed) {
        logger.info "Title: ${feed.title}"
        // TODO: Check for tombstones; if so, delete in depot.
        feed = feed.sortEntriesByUpdated(true)
        def currentUpdated = new Date() // TODO: choke on "futures" in collect source?
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
        /* TODO:
        * Fail on:
            javax.net.ssl.SSLPeerUnverifiedException
            java.net.SocketException
            MissingRdfContentException
            URIComputationException
            DuplicateDepotEntryException
            SourceCheckException (from SourceContent#writeTo), ...
          - then *rollback* (entire batch?)!
          - and *report error*

        throw new Exception("Bad MD5 checksum in " +
                depotEntry.getId()+" for "+mapEntry.getKey()+". Was "+
                mapEntry.getValue()+", expected "+storedMd5Hex+".")
        */
    }

    @Override
    public HttpClient createClient() {
        // TODO: Configure to use SSL (https) and verify cert.!
        // TODO:? httpClient.setHttpRequestRetryHandler(...)

        HttpParams params = new BasicHttpParams()
        ConnManagerParams.setMaxTotalConnections(params, 100)
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
        // FIXME: According to RFC 2616, 8.1.4, a client should nor maintain
        // more than 2 open connections per host. We *can* bump it up like this:
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(20))
        // .. but it's "wrong". What we need is to open and read as late as
        // possible (done in SourceContent).

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

    public void initialize() {
        super.initialize()
    }

    public void shutdown() {
        super.shutdown()
        getClient().getConnectionManager().shutdown()
    }

    protected boolean storeEntry(Feed sourceFeed, Entry sourceEntry) {

        URI sourceEntryId = sourceEntry.getId().toURI()
        logger.info "Reading Entry <${sourceEntryId}> ..."
        // TODO:? if metaIndex.get(sourceEntryId): if not newOrUpdated: continue..

        List contents = initialContents(sourceEntry)
        List enclosures = new ArrayList()
        fillContentsAndEnclosures(sourceEntry, contents, enclosures)

        URI newUri = computeOfficialUriInPlace(sourceEntryId, contents)

        verifyRdf(newUri, contents)

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
                .. and "source feed" is "same as last"? (indirected via rdf facts)?
            */
            if (sourceIsNotAnUpdate(sourceEntry, depotEntry)) {
                logger.info("Encountered collected entry <${sourceEntry.getId()}> at [" + 
                        sourceEntry.getUpdated()+"].")
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

        saveSourceMetaInfo(sourceFeed, sourceEntry, depotEntry)
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
        def inStream = getResponseAsInputStream(urlPath)
        /* FIXME: rethink opening connections and reading(+checking); write to
           tmp-file? Extend SourceContent with something "late-bound"? See
           createClient above.
        */
        /* FIXME: testing to read the response at once.. * /
        def outStream = new ByteArrayOutputStream()
        IOUtils.copyLarge(inStream, outStream)
        inStream = new ByteArrayInputStream(outStream.toByteArray())
        /* end */
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


    protected URI computeOfficialUriInPlace(
            URI sourceEntryId, List<SourceContent> contents) {
        def repo = RDFUtil.createMemoryRepository()

        def rdfContent = getRdfContent(sourceEntryId, contents)

        // FIXME: RDFa must be handled more manually (getting, esp. serializing!)
        // TODO:IMPROVE: lots of buffered data going on here; how to streamline?
        // TODO:IMPROVE: "" is baseURI; should be passed (via SourceContent?)?
        // TODO:IMPROVE: keep original RDF as metaFile?
        def rdfOutStream = new ByteArrayOutputStream()
        rdfContent.writeTo(rdfOutStream)
        RDFUtil.loadDataFromStream(repo,
                new ByteArrayInputStream(rdfOutStream.toByteArray()),
                "", rdfContent.mediaType)

        def newUri = uriMinter.computeOfficialUri(repo) // TODO: give sourceEntryId for subj?

        if (!sourceEntryId.equals(newUri)) {
            logger.info("New URI: <${newUri}>")
            repo = RDFUtil.replaceURI(repo, sourceEntryId, newUri)
        }
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

    protected void verifyRdf(URI docUri, List<SourceContent> contents) {
        // FIXME: use new base.RDFVerifyer#verifyRdf()
        // .. also, really read and re-set sourceContent? Write entry *first*?
        // .. or call this via computeOfficialUriInPlace..?
    }

}
