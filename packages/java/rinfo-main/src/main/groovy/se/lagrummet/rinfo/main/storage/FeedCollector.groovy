package se.lagrummet.rinfo.main.storage


import javax.xml.namespace.QName

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

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Link
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.Atomizer
import se.lagrummet.rinfo.store.depot.DepotEntry
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.collector.atom.FeedArchivePastToPresentReader


/* TODO: (see details in inline comments)

    - error recovery tests
    - error reporting

    ---- Documentation ----

    * gets all feeds until last collected, then reads entries forwards in time.

    * All entries read will have new timestamps based on their actual
      (successful) addition into the depot. This because the resulting feed
      must be ordered by "collect" time (not source times, since multiple
      sources have no interdependent ordering).

*/
class FeedCollector extends FeedArchivePastToPresentReader {

    public static final QName LINK_EXT_FILENAME = new QName(
            "http://rinfo.lagrummet.se/ns/2009/09/atom-extensions#",
            "filename", "rae");

    private final Logger logger = LoggerFactory.getLogger(FeedCollector)

    StorageSession storageSession

    /**
     * Important: this class is not thread safe. Create a new instance for each
     * collect session.
     */
    private FeedCollector(StorageSession storageSession) {
        this.storageSession = storageSession
    }

    public static void readFeed(StorageSession storageSession, URL url) {
        def collector = new FeedCollector(storageSession)
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
                storageSession.close()
            } finally {
                getClient().getConnectionManager().shutdown()
            }
        }
    }

    @Override
    boolean stopOnEntry(Entry entry) {
        return storageSession.hasCollected(entry)
    }

    @Override
    boolean hasVisitedArchivePage(URL pageUrl) {
        // TODO:? never visit pageUrl being an already visited archive page?
        return false
    }

    @Override
    public void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deletedMap) {
        logger.info("Processing feed page: <${pageUrl}> (id <${feed.id}>)")

        storageSession.beginPage(pageUrl, feed)
        try {
            for (entry in effectiveEntries) {
                try {
                    List<SourceContent> contents = new ArrayList<SourceContent>()
                    List<SourceContent> enclosures = new ArrayList<SourceContent>()
                    fillContentsAndEnclosures(sourceEntry, contents, enclosures)
                    storageSession.storeEntry(feed, entry, contents, enclosures)
                } catch (Exception e) {
                    // TODO:? storageSession should handle (log and report) errors..
                    logger.error("Critical error when storing entry: "+entry, e)
                    throw e
                }
            }
            deleteFromMarkers(feed, deletedMap)
        } finally {
            storageSession.endPage()
        }
    }

    protected void deleteFromMarkers(Feed sourceFeed, Map<IRI, AtomDate> deletedMap) {
        for (Map.Entry<IRI, AtomDate> delItem : deletedMap.entrySet()) {
            try {
                storageSession.deleteEntry(sourceFeed,
                        delItem.getKey().toURI(),
                        delItem.getValue().getDate())
            } catch (Exception e) {
                // TODO: report explicit error. storageSession should catch most though?
                logger.error("Error deleting entry!", e)
                throw e
            }
        }
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
                def slug = getEnclosureSlug(link, sourceEntry)
                enclosures.add(createSourceContent(
                        urlPath, mediaType, null, slug, md5hex, len))
            }
        }
    }

    protected SourceContent getMainContent(Entry sourceEntry) {
        def contentElem = sourceEntry.getContentElement()
        def contentUrlPath = contentElem.getResolvedSrc().toString()
        def contentMimeType = contentElem.getMimeType().toString()
        def contentLang = contentElem.getLanguage()
        def contentMd5hex = contentElem.getAttributeValue(Atomizer.LINK_EXT_MD5)
        // TODO: allow inline content!
        return createSourceContent(
                contentUrlPath, contentMimeType, contentLang, null, contentMd5hex)
    }

    protected SourceContent createSourceContent(urlPath,
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

    // TODO:? put these in a separate util class?

    protected static String getEnclosureSlug(Link atomLink) {
        return getEnclosureSlug(atomLink, (Entry)atomLink.getParentElement())
    }

    protected static String getEnclosureSlug(Link atomLink, Entry atomEntry) {
        return getEnclosureSlug(atomLink, atomEntry.getId())
    }

    protected static String getEnclosureSlug(Link atomLink, IRI entryId) {
        String slug = atomLink.getAttributeValue(LINK_EXT_FILENAME)
        if (slug != null) {
            // TODO: what forms do we support?
            return new URI(entryId.toString()+"/"+slug).getPath()
        } else {
            return computeEnclosureSlug(entryId.toURI(),
                    atomLink.resolvedHref.toURI())
        }
    }

    protected static String computeEnclosureSlug(URI entryUri, URI enclosureUri) {
        String entryIdBase = entryUri.getPath()
        String enclPath = enclosureUri.getPath()
        if (!enclPath.startsWith(entryIdBase)) {
            // TODO: fail with what?
            throw new RuntimeException("Entry <"+entryUri +
                    "> references <${enclosureUri}> out of its domain.")
        }
        return enclPath
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
