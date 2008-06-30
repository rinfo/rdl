package se.lagrummet.rinfo.store.depot

import org.apache.commons.io.FileUtils

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry


class DepotEntry {

    static final ENTRY_CONTENT_DIR_NAME = "ENTRY-INFO"
    static final MANIFEST_FILE_NAME = "manifest.xml"
    static final CONTENT_FILE_PATTERN = ~/content(?:-(\w{2}))?.(\w+)/
    static final DELETED_FILE_NAME = "DELETED"

    FileDepot depot
    String entryUriPath

    protected File entryDir
    protected File entryContentDir

    private Entry manifest

    DepotEntry(FileDepot depot, File entryDir, String knownUriPath,
            boolean mustExist=true) throws DeletedDepotEntryException {
        this.depot = depot
        this.entryDir = entryDir
        this.entryUriPath = knownUriPath
        entryContentDir = new File(entryDir, ENTRY_CONTENT_DIR_NAME)
        if (mustExist && isDeleted()) {
            throw new DeletedDepotEntryException(this)
        }
    }

    static boolean isEntryDir(File dir) {
        return new File(dir, ENTRY_CONTENT_DIR_NAME).isDirectory()
    }

    String getEntryUriPath() {
        if (!entryUriPath) {
            entryUriPath = computeEntryUriPath()
        }
        return entryUriPath
    }

    String computeEntryUriPath() {
        def uri = getId()
        assert depot.withinBaseUri(uri)
        return uri.path
    }

    URI getId() {
        return getEntryManifest().id.toURI()
    }

    Date getPublished() {
        return getEntryManifest().published
    }

    Date getUpdated() {
        return getEntryManifest().updated
    }

    boolean isDeleted() {
        return getDeletedMarkerFile().isFile()
    }

    List<DepotContent> findContents(String forMediaType=null, String forLang=null) {
        def found = []
        // TODO: if both qualifiers given, get file with newContentFile?
        for (File file : entryContentDir.listFiles()) {
            def match = CONTENT_FILE_PATTERN.matcher(file.name)
            if (!match.matches()) {
                continue
            }
            def mediaHint = match.group(2)
            def mediaType = depot.pathProcessor.mediaTypeForHint(mediaHint)
            if (forMediaType && mediaType != forMediaType) {
                continue
            }
            def lang = match.group(1) ?: null
            if (forLang && lang != forLang) {
                continue
            }
            // TODO: we now decouple suffix and hint logic:
            //  .. although this reintroduces map->remap->back-to-map..!
            //  .. and mediaHint *is* very concise.
            //  - receiving forMediaType in findContents
            //  - calling depot.mediaTypeForSuffix that forwards to mediaTypeForHint..
            //  .. could allow mediaHint to be collection-dependent in pathProcessor..
            def uriPath = depot.pathProcessor.makeNegotiatedUriPath(
                    getEntryUriPath(), mediaType, lang)
            found << new DepotContent(file, uriPath, mediaType, lang)
        }
        return found
    }

    List<DepotContent> findEnclosures() {
        // FIXME: Doesn't search sub-folders! Must also skip nested entries.
        // .. generic static "find stuff in dir" methods here?
        //    Sync with e.g. FileDepot#iterateEntries
        def enclosures = []
        for (File file : entryDir.listFiles()) {
            if (!file.hidden && file.isFile()) {
                def uriPath = toEnclosedUriPath(file)
                def mediaType = depot.computeMediaType(file)
                enclosures << new DepotContent(file, uriPath, mediaType)
            }
        }
        return enclosures
    }

    String toEnclosedUriPath(File file) {
        // TODO: depot.toUriPath(file .. or relativeFilePath..)
        def fileUriPath = file.toURI().toString()
        def entryDirUriPath = entryDir.toURI().toString()
        assert fileUriPath.startsWith(entryDirUriPath)
        def pathRemainder = fileUriPath.replaceFirst(entryDirUriPath, "")
        def uriPath = getEntryUriPath() + "/" + pathRemainder
        return uriPath
    }

    /**
     * The last system modification timestamp of this entry.
     */
    long lastModified() {
        return getManifestFile().lastModified()
    }

    String getContentMediaType() {
        return getEntryManifest().contentMimeType.toString()
    }

    String getContentLanguage() {
        def contentElem = getEntryManifest().contentElement
        if (contentElem == null || contentElem.language == null) {
            return null
        }
        return contentElem.language.toString()
    }

    protected Entry getEntryManifest() {
        if (!manifest) {
            manifest = Abdera.instance.parser.parse(
                    new FileInputStream(getManifestFile())).root
        }
        return manifest
    }

    protected getManifestFile() {
        return new File(entryContentDir, MANIFEST_FILE_NAME)
    }

    protected File getDeletedMarkerFile() {
        return new File(entryContentDir, DELETED_FILE_NAME)
    }


    //==== TODO: in WritableDepotEntry subclass? ====

    void create(Date createTime,
            List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures=null) {
        if(entryContentDir.exists()) {
            throw new DuplicateDepotEntryException(this)
        }
        entryContentDir.mkdir()
        def manifestFile = getManifestFile()
        def manifest = Abdera.instance.newEntry()
        // TODO: unify with rest of getId/entryPath stuff!
        manifest.id = depot.baseUri.resolve(getEntryUriPath())
        manifest.setPublished(createTime)
        manifest.setUpdated(createTime)
        setPrimaryContent(manifest, sourceContents)
        manifest.writeTo(new FileOutputStream(manifestFile))
        for (content in sourceContents) {
            addContent(content)
        }
        if (sourceEnclosures) {
            for (encl in sourceEnclosures) {
                addEnclosure(encl)
            }
        }
        depot.onEntryModified(this)
    }

    void update(Date updateTime,
            List<SourceContent> sourceContents=null,
            List<SourceContent> sourceEnclosures=null) {

        rollOffToHistory()

        if (sourceContents) {
            for (content in sourceContents) {
                addContent(content, true)
            }
        }
        if (sourceEnclosures) {
            for (encl in sourceEnclosures) {
                addEnclosure(encl, true)
            }
        }
        def manifest = getEntryManifest()
        manifest.setUpdated(updateTime)
        setPrimaryContent(manifest, sourceContents)
        manifest.writeTo(new FileOutputStream(getManifestFile()))
        depot.onEntryModified(this)
    }

    void delete(Date deleteTime) throws DeletedDepotEntryException {
        if (isDeleted()) {
            throw new DeletedDepotEntryException(this)
        }
        getDeletedMarkerFile().createNewFile()
        def manifest = getEntryManifest()
        manifest.setUpdated(deleteTime)

        // TODO: wipe content and enclosures
        // - but keep generated content.entry? (how to know that?)
        // .. (opt. move away..?)
        rollOffToHistory()

        manifest.writeTo(new FileOutputStream(getManifestFile()))
        depot.onEntryModified(this)
        // TODO: opt. mark "410 Gone" for enclosures?
    }

    // TODO: resurrect(...) (clears deleted state + (partial) create)

    void addContent(SourceContent srcContent, boolean replace=false) {
        def file = newContentFile(srcContent.mediaType, srcContent.lang)
        if (!replace) {
            assert !file.exists()
        }
        srcContent.writeTo(file)
    }

    File newContentFile(String mediaType, String lang=null) {
        def filename = "content"
        if (lang) {
            filename += "-" + lang
        }
        def suffix = depot.pathProcessor.hintForMediaType(
                mediaType)
        assert suffix // TODO: throw UnknownMediaTypeException?
        filename += ("." + suffix)
        return new File(entryContentDir, filename)
    }

    void addEnclosure(SourceContent srcContent, boolean replace=false) {
        // TODO: how shall enclosedUriPath be given? As full path?
        def enclUriPath = srcContent.enclosedUriPath
        assert enclUriPath.startsWith(entryUriPath)
        def enclPath = enclUriPath.replaceFirst(entryUriPath, "")
        def file = new File(entryDir, enclPath)
        if (!replace) {
            assert !file.exists()
        }
        // TODO: ... add in path..
        srcContent.writeTo(file)
    }

    protected void setPrimaryContent(Entry manifest,
            List<SourceContent> sourceContents) {
        if (sourceContents != null && sourceContents.size() > 0) {
            def content = sourceContents[0]
            manifest.setContent(null, content.mediaType)
            if (content.lang) {
                manifest.contentElement.language = content.lang
            }
        }
    }

    protected void rollOffToHistory() {
        // NOTE: this means existing content not in the update is removed.
        //      - *including enclosures*
        def historyDir = newHistoryDir()

        FileUtils.copyFileToDirectory(getManifestFile(), historyDir, true)

        for (content in findContents()) {
            FileUtils.moveToDirectory(content.file, historyDir, false)
        }
        for (enclContent in findEnclosures()) {
            // FIXME: could need to move entire dirs! protected findTopEnclosures?
            FileUtils.moveToDirectory(enclContent.file, historyDir, false)
        }

    }

    protected File newHistoryDir() {
        def dirPath = DatePathUtil.toEntryHistoryPath(getUpdated())
        def dir = new File(entryContentDir, dirPath)
        assert !dir.exists() && !dir.isDirectory()
        FileUtils.forceMkdir(dir)
        return dir
    }

}
