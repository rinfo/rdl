package se.lagrummet.rinfo.store.depot

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
/*
import org.apache.abdera.model.Collection
import org.apache.abdera.model.Service
import org.apache.abdera.model.Workspace
*/
import org.apache.abdera.ext.history.FeedPagingHelper as FPH

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.NameFileFilter

import static org.apache.commons.lang.time.DateFormatUtils.ISO_DATE_FORMAT


class FileDepot {

    private final logger = LoggerFactory.getLogger(FileDepot)

    URI baseUri
    File baseDir
    String feedPath

    // TODO: configure via metadata.. (IoC? Data in baseDir?)
    private UriPathProcessor pathProcessor = new UriPathProcessor()

    private File feedDir

    FileDepot() { }

    FileDepot(URI baseUri) {
        this.baseUri = baseUri
    }

    FileDepot(URI baseUri, File baseDir, String feedPath) {
        this(baseUri)
        this.setBaseDir(baseDir)
        this.setFeedPath(feedPath)
    }

    void setBaseDir(File baseDir) {
        this.baseDir = baseDir
        if (!baseDir.directory) {
            throw new ConfigurationException(
                    "Directory ${baseDir} does not exist.")
        }
    }

    void setFeedPath(String feedPath) {
        this.feedPath = feedPath
        this.feedDir = new File(baseDir, feedPath)
    }

    List<DepotContent> find(String uriPath) {
        def results = []

        def parsed = pathProcessor.parseUriPath(uriPath)
        if (!parsed) {
            return null
        }

        // TODO: Require suffix in req? And/or conneg?
        if (parsed.collection == feedPath) {
            def feed = getContent(uriPath + ".atom")
            if (!feed) {
                return null
            }
            return [feed]
        }

        def entry = getEntry(parsed.depotUriPath)
        if (entry) {
            def mediaType = pathProcessor.mediaTypeForHint(parsed.mediaHint)
            results = entry.findContents(mediaType, parsed.lang)
        } else { // enclosure..
            def content = getContent(parsed.depotUriPath)
            if (content) {
                results << content
            }
        }
        return results
    }

    // TODO: reintroduce EntryNotFoundException on !entryContentDir.isDir()?
    //  .. and/or EntryDeletedException?
    DepotEntry getEntry(String uriPath) {
        def entryDir = getEntryDir(uriPath)
        if (DepotEntry.isEntryDir(entryDir)) {
            return new DepotEntry(this, entryDir, uriPath)
        }
        return null
    }

    DepotEntry getEntry(URI entryUri) {
        assert withinBaseUri(entryUri) // TODO: UriNotWithinDepotException?
        return getEntry(entryUri.path)
    }

    DepotContent getContent(String uriPath) {
        def file = new File(baseDir, toFilePath(uriPath))
        if (!file.isFile()) {
            return null
        }
        def mediaType = computeMediaType(file)
        return new DepotContent(file, uriPath, mediaType)
    }

    Iterator<DepotEntry> iterateEntries() {
        def manifestIter = FileUtils.iterateFiles(baseDir,
                new NameFileFilter("manifest.xml"), DirectoryFileFilter.INSTANCE)

        return [

            hasNext: { manifestIter.hasNext() },

            next: {
                def entryDir
                while (manifestIter.hasNext()) {
                    def file = manifestIter.next()
                    def parentParent = file.parentFile.parentFile
                    if (DepotEntry.isEntryDir(parentParent)) {
                        entryDir = parentParent
                        break
                    }
                }
                if (!entryDir) {
                    throw new NoSuchElementException()
                }
                return new DepotEntry(this, entryDir)
            },

            remove: { throw new UnsupportedOperationException() }

        ] as Iterator<DepotEntry>
    }

    //========================================

    boolean withinBaseUri(URI uri) {
        return (uri.host == baseUri.host &&
                uri.scheme == baseUri.scheme &&
                uri.port == baseUri.port)
    }

    // FIXME: Knows *very* little! Configurable?
    String computeMediaType(File file) {
        def mtype = URLConnection.fileNameMap.getContentTypeFor(file.name)
        // TODO: this is too simple. Unify or only via some fileExtensionUtil..
        if (!mtype) {
            mtype = pathProcessor.mediaTypeForHint(file.name.split(/\./)[-1] )
        }
        return mtype
    }

    protected File getEntryDir(String uriPath) {
        return new File(baseDir, toFilePath(uriPath))
    }

    protected String toFilePath(String uriPath) {
        assert uriPath && uriPath[0] == "/"
        def localUriPath = uriPath.replaceFirst("/", "")
        // FIXME: do a smarter (probably reversable) algorithm!
        def path = localUriPath.replace(":", "/")
        path = path.split("/").collect { URLEncoder.encode(it) }.join("/")
        return path
    }

    protected String toFilePath(URI uri) {
        assert withinBaseUri(uri) // TODO: UriNotWithinDepotException?
        return toFilePath(uri.path)
    }


    //========================================

    DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures=null) {
        assert withinBaseUri(entryUri)
        def uriPath = entryUri.path
        def entryDir = getEntryDir(uriPath)
        FileUtils.forceMkdir(entryDir)
        def entry = new DepotEntry(this, entryDir, uriPath)
        entry.create(created, contents, enclosures)
        return entry
    }

    void onEntryModified(DepotEntry entry) {
        entry.generateAtomEntryContent()
        // TODO: update latest feed index file (may create new file and modify
        // next-to-last (add next-archive)?)
    }

    //==== TODO: in separate FileDepotAtomIndexStrategy / ...FeedIndexer? ====

    static final DEFAULT_FEED_BATCH_SIZE = 25
    int feedBatchSize

    void generateIndex() {
        if (!feedDir.exists()) {
            feedDir.mkdir()
        }
        def ascDateSortedEntryRefs = new TreeSet(
                [compare: { a, b ->
                    if (a.date == b.date) {
                        return a.uriPath.compareTo(b.uriPath)
                    }
                    return a.date.compareTo(b.date)
                }] as Comparator
            )
        // only keeping necessary data to minimize memory use
        for (entry in iterateEntries()) {
            ascDateSortedEntryRefs.add(
                    new EntryRef(uriPath: entry.entryUriPath,
                            date: entry.updated) )
        }
        indexEntryRefs(ascDateSortedEntryRefs)
    }

    int getFeedBatchSize() {
        return feedBatchSize ?: DEFAULT_FEED_BATCH_SIZE
    }

    // TODO: refactor and test algorithm in isolation?
    void indexEntryRefs(Collection<EntryRef> entryRefs) {

        def subscriptionPath = getSubscriptionPath()

        // FIXME: skip entries already indexed!
        //  how? climb all? Wipe and re-index (thus ignoring these "get existing")?
        // .. wiping controlled from generateIndex, this method should then add
        //    only new -- see "assure added entries younger" below
        def currFeed = getFeed(subscriptionPath)
        if (currFeed == null) {
            currFeed = newFeed(subscriptionPath)
        }
        def youngestArchFeed = getPrevArchiveAsFeed(currFeed)

        // TODO: assure added entries are younger than latest in currFeed?
        // Currently, indexEntryRefs re-indexes *if the same* are added..
        // .. if not, fail or re-index? Flag for this? *May* be critical
        // (Business-logic may *never* allow adding "older" ones)!

        def batchCount = currFeed.getEntries().size()
        for (EntryRef entryRef : entryRefs) {
            batchCount++
            def entry = getEntry(entryRef.uriPath)

            if (batchCount > getFeedBatchSize()) { // save as archive
                batchCount = 1 // popping added, but we have one new
                FPH.setArchive(currFeed, true)
                def archPath = pathToArchiveFeed(entry.updated) // youngest entry..
                currFeed.getSelfLink().setHref(archPath)
                FPH.setCurrent(currFeed, subscriptionPath)
                if (youngestArchFeed) {
                    FPH.setNextArchive(youngestArchFeed, uriPathFromFeed(currFeed))
                    writeFeed(youngestArchFeed) // re-write..
                }
                writeFeed(currFeed)
                youngestArchFeed = currFeed
                currFeed = newFeed(subscriptionPath)
            }

            if (youngestArchFeed) {
                FPH.setPreviousArchive(currFeed, uriPathFromFeed(youngestArchFeed))
            }

            logger.info "Indexing entry: <${entry.id}> [${entry.updated}]"

            indexEntry(currFeed, entry)

        }
        writeFeed(currFeed) // as subscription feed

    }

    String getSubscriptionPath() {
        // TODO: less hard-coded..
        "/${feedPath}/current"
    }

    void indexEntry(Feed feed, DepotEntry entry) {
        // FIXME: handle entry.deleted ... !
        def entryFile = entry.generateAtomEntryContent(false)
        def atomEntry = Abdera.instance.parser.parse(
                new FileInputStream(entryFile)).root
        feed.insertEntry(atomEntry)
    }

    protected Feed newFeed(String uriPath) {
        def feed = Abdera.instance.newFeed()
        // FIXME: metadata for feed basics: baseFeed.clone()
        //feed.id = baseUri
        //feed.title = null
        feed.setUpdated(new Date()) // TODO: which utcDateTime?
        feed.addLink(uriPath, "self")
        return feed
    }

    protected Feed getFeed(String uriPath) {
        def file = new File(baseDir, toFeedFilePath(uriPath))
        if (!file.exists()) {
            return null
        }
        return Abdera.instance.parser.parse(new FileInputStream(file)).root
    }

    protected Feed getPrevArchiveAsFeed(Feed feed) {
        def prev = FPH.getNextArchive(feed)
        if (!prev) {
            return null
        }
        return getFeed(prev.toString())
    }

    protected Feed getFeedForDateTime(Date date) {
        // TODO: to use for e.g. "emptying" deleted entries
        // - search in feed folder by date, time; opt. offset (can there be many in same same instant?)
        // .. getFeedForDateTime(pathToArchiveFeed(date))
        return null
    }

    protected String uriPathFromFeed(Feed feed) {
        return feed.getSelfLink().getHref().toString()
    }

    protected String pathToArchiveFeed(Date youngestDate) {
        // TODO: offset in batch "in date".. And date as subdirs?
        def isoDate = ISO_DATE_FORMAT.format(youngestDate)
        return "/${feedPath}/${isoDate}"
    }

    protected String toFeedFilePath(String uriPath) {
        // TODO: less hard-coded..
        return toFilePath(uriPath) + ".atom"
    }

    protected void writeFeed(Feed feed) {
        def uriPath = uriPathFromFeed(feed)
        logger.info "Writing feed: <${uriPath}>"
        def feedFileName = toFeedFilePath(uriPath)
        feed.writeTo(new FileOutputStream(new File(baseDir, feedFileName)))
    }

}

protected class EntryRef {
    String uriPath
    Date date
}
