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

import se.lagrummet.rinfo.collector.atom.CompleteFeedEntryIdIndex;
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

    /**
     * Important: this class is not thread safe. Create a new instance for each
     * collect session.
     */
    public FeedCollectorSession(HttpClient httpClient,
            StorageSession storageSession) {
        setClient(httpClient)
        this.storageSession = storageSession
        this.entryPathMapper = new EntryPathMapper(
                storageSession.getDepot().getPathHandler())
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
    public boolean stopOnEntry(Entry entry) {
        return storageSession.hasCollected(entry)
    }

    @Override
    public boolean hasVisitedArchivePage(URL pageUrl) {
        // TODO:? never visit pageUrl being an already visited archive page?
        return false
    }

    @Override
    public CompleteFeedEntryIdIndex getCompleteFeedEntryIdIndex() {
        return storageSession.getCompleteFeedEntryIdIndex()
    }

    @Override
    public void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deletedMap) {
        logger.info("Processing feed page: <${pageUrl}> (id <${feed.id}>)")

        storageSession.beginPage(pageUrl, feed)
        try {
            deleteFromMarkers(feed, deletedMap)
            for (entry in effectiveEntries) {
                try {
                    List<SourceContent> contents = new ArrayList<SourceContent>()
                    List<SourceContent> enclosures = new ArrayList<SourceContent>()
                    fillContentsAndEnclosures(entry, contents, enclosures)
                    def ok = storageSession.storeEntry(
                            feed, entry, contents, enclosures)
                    if (!ok) {
                        break
                    }
                } catch (Exception e) {
                    // NOTE: storageSession should handle (log and report) errors.
                    logger.error("Critical error when storing entry: "+entry, e)
                    throw e
                }
            }
        } finally {
            storageSession.endPage(pageUrl)
        }
    }

    protected void deleteFromMarkers(Feed sourceFeed, Map<IRI, AtomDate> deletedMap) {
        for (Map.Entry<IRI, AtomDate> delItem : deletedMap.entrySet()) {
            try {
                storageSession.deleteEntry(sourceFeed,
                        delItem.getKey().toURI(),
                        delItem.getValue().getDate())
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
            def md5hex = link.getAttributeValue(Atomizer.LINK_EXT_MD5)
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
        if (contentElem.getSrc() == null) {
            return null
        }
        def contentUrlPath = contentElem.getResolvedSrc().toString()
        def contentMimeType = contentElem.getMimeType().toString()
        def contentLang = contentElem.getLanguage()
        def contentMd5hex = contentElem.getAttributeValue(Atomizer.LINK_EXT_MD5)
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

    private FeedCollectorSession collector;
    String urlPath;

    public RemoteSourceContent(FeedCollectorSession collector, String urlPath,
            String mediaType, String lang, String enclosedUriPath) {
        super(mediaType, lang, enclosedUriPath);
        this.collector = collector;
        this.urlPath = urlPath;
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        // TODO:IMPROVE: retrying http; and also handle failed gets in writeTo(File)
        if (getSourceStream() == null) {
            setSourceStream(collector.getResponseAsInputStream(urlPath), false);
        }
        super.writeTo(outStream);
    }

}
