package se.lagrummet.rinfo.store.depot

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Collection
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Service
import org.apache.abdera.model.Workspace
import org.apache.abdera.ext.history.FeedPagingHelper as FPH

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.NameFileFilter

import static org.apache.commons.lang.time.DateFormatUtils.ISO_DATE_FORMAT


class FileDepot {

    final logger = LoggerFactory.getLogger(FileDepot)

    URI baseUri
    File baseDir

    private File feedDir
    // TODO: configure via metadata.. (IoC? Data in baseDir?)
    private DepotUriStrategy uriStrategy = new DepotUriStrategy()

    FileDepot() { }

    FileDepot(URI baseUri) {
        this.baseUri = baseUri
    }

    FileDepot(URI baseUri, File baseDir) {
        this(baseUri)
        this.setBaseDir(baseDir)
    }

    void setBaseDir(File baseDir) {
        this.baseDir = baseDir
        if (!baseDir.directory) {
            throw new ConfigurationException(
                    "Directory ${baseDir} does not exist.")
        }
        // TODO: how to configure?
        this.feedDir = new File(baseDir, uriStrategy.FEED_DIR_NAME)
    }

    List<DepotContent> find(String uriPath) {
        def results = []

        def parsed = uriStrategy.parseUriPath(uriPath)
        if (!parsed) {
            return null
        }

        // TODO: very crude; rework how?
        if (parsed.collection == uriStrategy.FEED_DIR_NAME) {
            def feed = getContent(uriPath + ".atom")
            if (!feed) {
                return null
            }
            return [feed]
        }

        def entry = getEntry(parsed.depotUriPath)
        if (entry) {
            results = entry.findContents(parsed.mediaHint, parsed.lang)
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
            mtype = uriStrategy.mediaTypeForHint(file.name.split(/\./)[-1] )
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


    //========================================

    DepotEntry createEntry(URI entryUri, Date created,
            List<DepotContent> contents,
            List<DepotContent> enclosures=null) {
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
        // next-to-last?)
    }

    //==== TODO: in separate FileDepotAtomIndexer? ====

    static final FEED_BATCH_SIZE = 25

    void generateIndex() {

        if (!feedDir.exists()) {
            feedDir.mkdir()
        }

        def ascDateSortedEntries = new TreeSet(
                { a, b ->
                    if (a.date == b.date) {
                        return a.path.compareTo(b.path)
                    }
                    return a.date.compareTo(b.date)
                } as Comparator
            )

        for (entry in iterateEntries()) {
            ascDateSortedEntries.add(
                    [ path: entry.entryUriPath,
                      date: entry.updated ]
                )
        }

        def totalEntryCount = ascDateSortedEntries.size()

        // FIXME: refactor and test algorithm in isolation!
        // FIXME: this totally doesn't work properly yet:
        //  - needs "prev+next", perhaps prevBatch, batch, nextBatch?
        //  - doesn't yet output complete subscriptionPath

        def subscriptionPath = "/${uriStrategy.FEED_DIR_NAME}/current"
        def batch = []
        def nextBatch = []
        def lastWrittenArchivePath = null
        def onLastBatch = false
        for (entryInfo in ascDateSortedEntries) {
            if (batch.size() < FEED_BATCH_SIZE) {
                batch << getEntry(entryInfo.path)
            } else {
                nextBatch << getEntry(entryInfo.path)
            }
            if (nextBatch.size() == FEED_BATCH_SIZE) {
                lastWrittenArchivePath = writeEntryBatchPair(
                        subscriptionPath, lastWrittenArchivePath,
                        batch, nextBatch, onLastBatch)
                batch = []
                nextBatch = []
            }
        }
        if (batch) {
            lastWrittenArchivePath = writeEntryBatchPair(
                    subscriptionPath, lastWrittenArchivePath,
                    batch, nextBatch, true)
        }
    }

    protected String writeEntryBatchPair(
            String subscriptionPath,
            String lastWrittenArchivePath,
            List<DepotEntry> batch,
            List<DepotEntry> nextBatch,
            boolean onLastBatch) {
        if (onLastBatch) {
            writeEntryBatch(batch,
                    subscriptionPath,
                    pathToBatch(batch[-1].updated),
                    lastWrittenArchivePath,
                    null
                )
            return null
        } else {
            def pathToNextBatch = nextBatch ?
                    pathToBatch(nextBatch[-1].updated) : null
            writeEntryBatch(batch,
                    subscriptionPath,
                    pathToBatch(batch[-1].updated),
                    lastWrittenArchivePath,
                    pathToNextBatch,
                )
            writeEntryBatch(nextBatch,
                    subscriptionPath,
                    pathToNextBatch,
                    pathToBatch(batch[-1].updated),
                    null // TODO: from next round (unless next is subsc)
                )
            return pathToNextBatch
        }
    }

    protected String pathToBatch(Date youngestDate) {
        // TODO: offset in batch "in date".. And date as subdirs?
        def isoDate = ISO_DATE_FORMAT.format(youngestDate)
        return "/${uriStrategy.FEED_DIR_NAME}/${isoDate}"
    }

    protected void writeEntryBatch(List<DepotEntry> batch,
            String subscriptionPath,
            String thisArchivePath,
            String prevArchivePath,
            String nextArchivePath) {
        def feed = Abdera.instance.newFeed()

        // FIXME: metadata for feed basics: baseFeed.clone()
        feed.id = baseUri
        feed.title = null

        def selfPath = thisArchivePath ?: subscriptionPath
        feed.addLink("self", selfPath)

        if (thisArchivePath) {
            logger.info "Writing feed: <${selfPath}>"
            FPH.setArchive(feed, true)
            FPH.setCurrent(feed, subscriptionPath)
            if (nextArchivePath) {
                FPH.setNextArchive(feed, nextArchivePath)
            }
        }
        if (prevArchivePath) {
            FPH.setPreviousArchive(feed, prevArchivePath)
        }

        for (entry in batch) {
            logger.info "Indexing entry: <${entry.id}> [${entry.updated}]"
            // TODO; if entry.deleted ... !
            feed.insertEntry(entry.parsedAtomEntry)
        }
        def feedFileName = toFilePath(selfPath) + ".atom"
        feed.writeTo(new FileOutputStream(new File(baseDir, feedFileName)))
    }

    protected void indexEntry(DepotEntry entry) {
        // TODO
    }

}
