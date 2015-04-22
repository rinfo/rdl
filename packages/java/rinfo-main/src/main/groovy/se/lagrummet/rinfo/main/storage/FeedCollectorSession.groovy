package se.lagrummet.rinfo.main.storage


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.http.client.HttpClient

import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Feed
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.Atomizer
import se.lagrummet.rinfo.store.depot.SourceContent

import se.lagrummet.rinfo.collector.atom.FeedEntryDataIndex
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
public class FeedCollectorSession extends FeedArchivePastToPresentReader {

    private final Logger logger = LoggerFactory.getLogger(FeedCollectorSession)

    StorageSession storageSession
    EntryPathMapper entryPathMapper

    private Atomizer atomizer

    /**
     * Important: this class is not thread safe. Create a new instance for each
     * collect session.
     */
    public FeedCollectorSession(HttpClient httpClient,
            StorageSession storageSession) {
        super(httpClient)
        this.atomizer = new Atomizer(readLegacyMd5LinkExtension: true)
        this.storageSession = storageSession
        this.entryPathMapper = new EntryPathMapper(
                storageSession.getDepot().getPathHandler())
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown()
        } finally {
            storageSession.close()
        }
    }

    @Override
    public boolean stopOnEntry(Entry entry) {
        return storageSession.hasCollected(entry)
    }

    @Override
    public boolean hasVisitedArchivePage(URL pageUrl) {
        // TODO:IMPROVE? return true if archive page in collector log?
        // And/or do a conditional get based on logged *complete* feed URL
        return false
    }

    @Override
    public FeedEntryDataIndex getFeedEntryDataIndex() {
        return storageSession.getFeedEntryDataIndex()
    }

    @Override
    public URL readFeedPage(URL pageUrl) throws IOException {
        try {
            return super.readFeedPage(pageUrl)
        } catch (Exception e) {
            logger.error("Critical error when processing feed page: " + pageUrl, e)
            handlePageError(e, pageUrl)
            return null
        }
    }

    @Override
    public boolean processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deletedMap) {
        logger.info("Processing feed page: <${pageUrl}> (id <${feed.id}>)")

        boolean ok = true;
        try {
            storageSession.beginPage(pageUrl, feed)
            deleteFromMarkers(feed, deletedMap)
            for (entry in effectiveEntries) {
                logger.debug("Processing feed entry: "+entry.getId())
                try {
                    List<SourceContent> contents = new ArrayList<SourceContent>()
                    List<SourceContent> enclosures = new ArrayList<SourceContent>()
                    fillContentsAndEnclosures(entry, contents, enclosures)
                    ok = storageSession.storeEntry(
                            feed, entry, contents, enclosures)
                    if (!ok) {
                        //todo log remaining entries
                        break
                    }

                } catch (Exception e) {
                    // NOTE: storageSession should handle (log and report) errors.
                    ok = false;
                    logger.error("Critical error when storing entry: " + entry, e)
                    throw e
                }
            }
        } catch (Exception e) {
            handlePageError(e, pageUrl)
        } finally {
            storageSession.endPage(pageUrl)
        }
        return ok;
    }

    protected void handlePageError(Exception e, URL pageUrl) {
        storageSession.onPageError(e, pageUrl)
        logger.error("Error reading feed page <"+ pageUrl +">", e)
    }

    protected void deleteFromMarkers(Feed sourceFeed, Map<IRI, AtomDate> deletedMap) {
        for (Map.Entry<IRI, AtomDate> delItem : deletedMap.entrySet()) {
            try {
                storageSession.deleteEntry(sourceFeed,
                        delItem.getKey().toURI(),
                        delItem.getValue().getDate())
                logger.debug("Deleted entry "+delItem.key+" in soruce feed "+sourceFeed.getBaseUri())
            } catch (Exception e) {
                // NOTE: storageSession should handle (log and report) errors.
                logger.error("Error deleting entry!", e)
                throw e
            }
        }
    }

    protected void fillContentsAndEnclosures(Entry sourceEntry,
            List contents, List enclosures) {
        def content = getMainContent(sourceEntry)
        if (content != null) {
            contents.add(content)
        }
        for (link in sourceEntry.links) {
            def urlPath = link.resolvedHref.toString()
            def mediaType = link.mimeType.toString()
            def lang = link.hrefLang
            long len = link.getLength()
            def md5hex = atomizer.getChecksums(link)["md5"]
            if (link.rel == "alternate") {
                contents.add(createSourceContent(
                        urlPath, mediaType, lang, null, md5hex, len))
            } else if (link.rel == "enclosure") {
                def slug = entryPathMapper.getEnclosureSlug(link, sourceEntry)
                enclosures.add(createSourceContent(
                        urlPath, mediaType, null, slug, md5hex, len))
            }
        }
    }

    protected SourceContent getMainContent(Entry sourceEntry) {
        def contentElem = sourceEntry.getContentElement()
        // TODO:? skip all inline content? Allow at least inline RDF?
        if (contentElem == null || contentElem.getSrc() == null) {
            return null
        }
        def contentUrlPath = contentElem.getResolvedSrc().toString()
        def contentMimeType = contentElem.getMimeType().toString()
        def contentLang = contentElem.getLanguage()
        def contentMd5hex = atomizer.getChecksums(contentElem)["md5"]
        return createSourceContent(
                contentUrlPath, contentMimeType, contentLang, null, contentMd5hex)
    }

    protected SourceContent createSourceContent(urlPath,
            mediaType, lang, slug=null,
            md5hex=null, length=-1) {
        def srcContent = new RemoteSourceContent(
                this, urlPath, mediaType, lang, slug)
        if (md5hex != null) {
            srcContent.datachecks[SourceContent.Check.MD5] = md5hex
        }
        if (length != -1) {
            srcContent.datachecks[SourceContent.Check.LENGTH] = length
        }
        return srcContent
    }

}


public class RemoteSourceContent extends SourceContent {

    private static final String MIME_TYPE_PDF = "application/pdf";

    private FeedCollectorSession collectorSession;
    String urlPath;
    String mediaType;

    private final Logger logger = LoggerFactory.getLogger(RemoteSourceContent)

    public RemoteSourceContent(FeedCollectorSession collectorSession, String urlPath,
            String mediaType, String lang, String enclosedUriPath) {
        super(mediaType, lang, enclosedUriPath);
        this.collectorSession = collectorSession;
        this.urlPath = urlPath;
        this.mediaType = mediaType;
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        if (getSourceStream() == null) {
            setSourceStream(collectorSession.getResponseAsInputStream(urlPath), false);
            super.writeTo(outStream);
        }
    }
}
