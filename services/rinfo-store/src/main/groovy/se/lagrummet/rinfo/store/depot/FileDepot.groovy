package se.lagrummet.rinfo.store.depot

import org.apache.abdera.Abdera
import org.apache.abdera.model.Collection
import org.apache.abdera.model.Entry
import org.apache.abdera.model.Service
import org.apache.abdera.model.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.NameFileFilter


class FileDepot {

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
        assert !entryDir.exists() // TODO: DuplicateEntryException?
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


    //========================================

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

    void generateIndex() {
        // TODO: write feed index files.

        def entryPathDates = [:]
        for (entry in iterateEntries()) {
            entryPathDates[entry.entryUriPath] = entry.updated
        }
        // TODO: entryPathDates as some Sorted..
        def ascendingEntryPaths = entryPathDates.
                entrySet().sort{ it.value }.collect{ it.key }

        def batch = []
        def batchSize = 100
        for (entryPath in ascendingEntryPaths) {
            batch << getEntry(entryPath)
            if (batch.size() == batchSize) {
                writeEntryBatch(batch)
                batch = []
            }
        }
        if (batch) {
            writeEntryBatch(batch)
        }
    }

    void writeEntryBatch(List<DepotEntry> batch) {
        def feed = Abdera.instance.newFeed()
        // FIXME: metadata for feed basics: baseFeed.clone()
        feed.id = baseUri
        feed.title = null
        for (entry in batch) {
            // FIXME: logger.info!
            println "Indexing entry: <${entry.id}> [${entry.updated}]"
            // TODO; if entry.deleted ... !
            feed.insertEntry(entry.parsedAtomEntry)
        }
        // TODO: filename from ... first date? + offset in batch "in date"..
        def feedFileName = "latest.atom"
        feed.writeTo(new FileOutputStream(new File(feedDir, feedFileName)))
    }

    void indexEntry(DepotEntry entry) {
        // TODO
    }

}
