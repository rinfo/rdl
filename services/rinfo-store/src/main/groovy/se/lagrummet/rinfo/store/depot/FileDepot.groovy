package se.lagrummet.rinfo.store.depot

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.NameFileFilter

import static org.apache.commons.lang.time.DateFormatUtils.ISO_DATE_FORMAT


class FileDepot {

    private final logger = LoggerFactory.getLogger(FileDepot)

    URI baseUri
    File baseDir
    String feedPath
    Atomizer atomizer

    // TODO: configure via metadata.. (IoC? Data in baseDir?)
    private UriPathProcessor pathProcessor = new UriPathProcessor()

    FileDepot() {
        this.atomizer = new Atomizer(this)
    }

    FileDepot(URI baseUri) {
        this()
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


    //== Feed Related ==

    int getFeedBatchSize() {
        return atomizer.feedBatchSize
    }

    void setFeedBatchSize(int batchSize) {
        atomizer.feedBatchSize = batchSize
    }

    DepotContent getFeedContent(String uriPath) {
        // TODO: Require suffix in req? And/or conneg?
        return getContent(uriPath + ".atom")
    }

    String getSubscriptionPath() {
        // TODO: less hard-coded..
        "/${feedPath}/current"
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


    //== Entry and Content Lookups ==

    List<DepotContent> find(String uriPath) {
        def results = []

        def parsed = pathProcessor.parseUriPath(uriPath)
        if (!parsed) {
            return null
        }

        if (parsed.collection == feedPath) {
            def feed = getFeedContent(uriPath)
            if (!feed) {
                return null
            }
            return [feed]
        }

        def depotEntry = getEntry(parsed.depotUriPath)
        if (depotEntry) {
            def mediaType = pathProcessor.mediaTypeForHint(parsed.mediaHint)
            results = depotEntry.findContents(mediaType, parsed.lang)
        } else { // enclosure..
            def content = getContent(parsed.depotUriPath)
            if (content) {
                results << content
            }
        }
        return results
    }

    // TODO: reintroduce EntryNotFoundException on !entryContentDir.isDir()?
    DepotEntry getEntry(String uriPath, mustExist=true)
            throws DeletedDepotEntryException {
        def entryDir = getEntryDir(uriPath)
        if (DepotEntry.isEntryDir(entryDir)) {
            return new DepotEntry(this, entryDir, uriPath, mustExist)
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

    Iterator<DepotEntry> iterateEntries(
            boolean includeHistorical=false, boolean includeDeleted=false) {

        def manifestIter = FileUtils.iterateFiles(baseDir,
                new NameFileFilter("manifest.xml"), DirectoryFileFilter.INSTANCE)
        return [

            hasNext: { manifestIter.hasNext() },

            next: {
                while (manifestIter.hasNext()) {
                    def file = manifestIter.next()
                    def parentParent = file.parentFile.parentFile
                    /* TODO:
                    if (includeHistorical) ... if (!isEntryDir(historical) ...)
                    */
                    if (DepotEntry.isEntryDir(parentParent)) {
                        def entryDir = parentParent
                        def depotEntry = new DepotEntry(
                                this, entryDir, null, false)
                        if (!includeDeleted && depotEntry.isDeleted()) {
                            continue
                        }
                        return depotEntry
                    }
                }
                throw new NoSuchElementException()
            },

            remove: { throw new UnsupportedOperationException() }

        ] as Iterator<DepotEntry>
    }

    //== Path and File Related ==

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

    //== Writing Specifics ==

    DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures=null) {
        assert withinBaseUri(entryUri)
        def uriPath = entryUri.path
        def entryDir = getEntryDir(uriPath)
        FileUtils.forceMkdir(entryDir)
        def depotEntry = new DepotEntry(this, entryDir, uriPath)
        depotEntry.create(created, contents, enclosures)
        return depotEntry
    }

    void onEntryModified(DepotEntry depotEntry) {
        atomizer.generateAtomEntryContent(depotEntry)
        // TODO: update latest feed index file (may create new file and modify
        // next-to-last (add next-archive)?)! Since any modifying means
        // a new updated depotEntry in the feeds..
    }

    void generateIndex() {
        atomizer.generateIndex()
    }

    // TODO: indexNewEntries? Or..
    // TODO: getEntrySession() .. for multiple operations (replace onEntryModified..)
    //  .. and allow implicit use in edtiting (for one-offs)

}
