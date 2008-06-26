package se.lagrummet.rinfo.store.depot

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI


class DepotEntry {

    static final ENTRY_CONTENT_DIR_NAME = "ENTRY-INFO"
    static final MANIFEST_FILE_NAME = "manifest.xml"
    static final CONTENT_FILE_PATTERN = ~/content(?:-(\w{2}))?.(\w+)/
    static final ATOM_ENTRY_MEDIA_TYPE = "application/atom+xml;type=entry"

    FileDepot depot
    String entryUriPath

    protected File entryDir
    protected File entryContentDir

    private Entry manifest

    DepotEntry(depot, entryDir, knownUriPath=null) {
        this.depot = depot
        this.entryDir = entryDir
        this.entryUriPath = knownUriPath
        entryContentDir = new File(entryDir, ENTRY_CONTENT_DIR_NAME)
    }

    static boolean isEntryDir(File dir) {
        return new File(dir, ENTRY_CONTENT_DIR_NAME).isDirectory()
    }

    List<DepotContent> findContents(String forMediaType=null, String forLang=null) {
        def found = []
        // TODO: if both qualifiers given, look for file with newContentFile?
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
        // FIXME:
        // Use updated and isDeleted!
        // but store feedsync internally - deleteds *may* de desirable to
        // "dry out" even in archive docs!
        return false
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

    List<DepotContent> findEnclosures() {
        // FIXME: Doesn't search sub-folders! Must also skip nested entries.
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


    // TODO: Thought: verify mtype on content, and derive mtype from
    // enclosures? Or do in use of filedepot (the collector)?

    void create(Date created,
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
        manifest.setPublished(created)
        manifest.setUpdated(created)
        if (sourceContents.size() > 0) {
            def content = sourceContents[0]
            manifest.setContent(null, content.mediaType)
            if (content.lang) {
                manifest.contentElement.language = content.lang
            }
        }
        manifest.writeTo(new FileOutputStream(manifestFile))
        for (content in sourceContents) { addContent(content) }
        if (sourceEnclosures) {
            for (encl in sourceEnclosures) { addEnclosure(encl) }
        }
        depot.onEntryModified(this)
    }

    void update(Date updated,
            List<SourceContent> sourceContents=null,
            List<SourceContent> sourceEnclosures=null) {
        def manifest = getEntryManifest()
        manifest.setUpdated(updated)
        manifest.writeTo(new FileOutputStream(getManifestFile()))
        // TODO: move content and enclosures into HISTORY_DIR..
        if (sourceContents) {
            for (content in sourceContents) { addContent(content, true) }
        }
        if (sourceEnclosures) {
            for (encl in sourceEnclosures) { addEnclosure(encl, true) }
        }
        depot.onEntryModified(this)
    }

    void delete(Date deleted) {
        // assert !isDeleted()
        def manifest = getEntryManifest()
        manifest.updated = deleted
        manifest.writeTo(new FileOutputStream(getManifestFile()))
        // TODO: wipe content (or move?)
        depot.onEntryModified(this)
        // TODO: how to "410 Gone" for enclosures..?
    }

    void addContent(SourceContent srcContent, boolean replace=false) {
        def file = newContentFile(srcContent.mediaType, srcContent.lang)
        if (!replace) {
            assert !file.exists()
        }
        srcContent.writeTo(file)
    }

    protected File newContentFile(String mediaType, String lang=null) {
        def filename = "content"
        if (lang) {
            filename += "-" + lang
        }
        def suffix = depot.pathProcessor.hintForMediaType(
                mediaType)
        assert suffix // TODO: throw NotAllowedContentMediaTypeException?
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

    //==== TODO: in separate FileDepotAtomIndexStrategy? ====

    // TODO: .. use a "generatedContentTypes" list that callbacks to get it (which
    // also blocks from adding content with that mediatype)?

    File generateAtomEntryContent(boolean force=true) {

        def entryFile = newContentFile(ATOM_ENTRY_MEDIA_TYPE)
        if (!force &&
            entryFile.isFile() &&
            entryFile.lastModified() > getManifestFile().lastModified()) {
           return entryFile
        }

        // TODO: how to represent deleted tombstones!
        def atomEntry = Abdera.instance.newEntry()
        // TODO: getEntryManifest().clone() ?
        atomEntry.id = getId()
        def publDate = getPublished()
        if (publDate) {
            atomEntry.setPublished(publDate)
        }
        atomEntry.setUpdated(getUpdated())

        // TODO: what to use as values?
        atomEntry.setTitle("")//getId().toString())
        atomEntry.setSummary("")//getId().toString())

        def selfUriPath = depot.pathProcessor.makeNegotiatedUriPath(
                getEntryUriPath(), ATOM_ENTRY_MEDIA_TYPE)
        atomEntry.addLink(selfUriPath, "self")

        // TODO: add md5 (link extension) or sha (xml-dsig)?

        // TODO: is this a reasonable mediaType()+lang) control?
        def contentMediaType = getEntryManifest().contentMimeType.toString()
        def contentLang
        def contentElem = getEntryManifest().contentElement
        if (contentElem && contentElem.language) {
            contentLang = contentElem.language.toString()
        }
        def contentIsSet = false
        for (content in findContents()) {
            if (content.mediaType == ATOM_ENTRY_MEDIA_TYPE) {
                continue
            }
            if (!contentIsSet
                && content.mediaType == contentMediaType
                && content.lang == contentLang) {
                atomEntry.setContent(new IRI(content.depotUriPath),
                        content.mediaType)
                if (content.lang) {
                    atomEntry.contentElement.language = content.lang
                }
                contentIsSet = true
            } else {
                atomEntry.addLink(content.depotUriPath,
                        "alternate",
                        content.mediaType,
                        null, // title
                        content.lang,
                        content.file.length())
            }
        }

        for (enclContent in findEnclosures()) {
            atomEntry.addLink(enclContent.depotUriPath,
                    "enclosure",
                    enclContent.mediaType,
                    null, // title
                    enclContent.lang,
                    enclContent.file.length())
        }

        atomEntry.writeTo(
                new FileOutputStream(entryFile))
        return entryFile
    }

}
