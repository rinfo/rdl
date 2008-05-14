package se.lagrummet.rinfo.store.depot

import org.apache.commons.io.FileUtils

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

    List<DepotContent> findContents(String forMediaHint=null, String forLang=null) {
        def found = []
        for (File file : entryContentDir.listFiles()) {
            def match = CONTENT_FILE_PATTERN.matcher(file.name)
            if (!match.matches()) {
                continue
            }
            def mediaHint = match.group(2)
            if (forMediaHint && mediaHint != forMediaHint) {
                continue
            }
            def lang = match.group(1) ?: null
            if (forLang && lang != forLang) {
                continue
            }
            // TODO: possibly decouple suffix and hint logic by:
            //  .. (although this reintroduces map->remap->back-to-map..!)
            //  .. (and mediaHint *is* very concise)
            //  - receiving forMediaType in findContents
            //  - calling depot.mediaTypeForSuffix that forwards to mediaTypeForHint..
            //  .. could allow mediaHint to be collection-dependent in uriStrategy..
            def mediaType = depot.uriStrategy.mediaTypeForHint(mediaHint)
            def uriPath = depot.uriStrategy.makeNegotiatedUriPath(
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

    Date getDeleted() {
        // FIXME: date from where? feedsync? app:edited?
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


    //==== TODO: in separate DepotEntryWriter? ====
    // Also: verify mtype on content, and derive mtype from
    // enclosures? Or do in use of filedepot (the collector)?

    void create(Date created,
            List<DepotContent> contents,
            List<DepotContent> enclosures=null) {
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
        if (contents.size() > 0) {
            def content = contents[0]
            manifest.setContent(null, content.mediaType)
            if (content.lang) {
                manifest.contentElement.language = content.lang
            }
        }
        manifest.writeTo(new FileOutputStream(manifestFile))
        for (content in contents) { addContent(content) }
        if (enclosures) {
            for (encl in enclosures) { addEnclosure(encl) }
        }
        depot.onEntryModified(this)
    }

    void addContent(DepotContent content, boolean replace=false) {
        def file = newContentFile(content.mediaType, content.lang)
        if (!replace) {
            assert !file.exists()
        }
        FileUtils.copyFile(content.file, file)
    }

    protected File newContentFile(String mediaType, String lang=null) {
        def filename = "content"
        if (lang) {
            filename += "-" + lang
        }
        def suffix = depot.uriStrategy.hintForMediaType(
                mediaType)
        assert suffix // TODO: throw NotAllowedContentMediaTypeException?
        filename += ("." + suffix)
        return new File(entryContentDir, filename)
    }

    void addEnclosure(DepotContent enclosure, boolean replace=false) {
        if (!replace) {
            assert !file.exists()
        }
        // FIXME: ... add in path..
    }

    void update(Date updated,
            List<DepotContent> contents=null,
            List<DepotContent> enclosures=null) {
        def manifest = getEntryManifest()
        manifest.setUpdated(updated)
        manifest.writeTo(new FileOutputStream(getManifestFile()))
        if (contents) {
            for (content in contents) { addContent(content, true) }
        }
        if (enclosures) {
            for (encl in enclosures) { addEnclosure(encl, true) }
        }
        depot.onEntryModified(this)
    }

    void delete(Date deleted) {
        // assert !isDeleted()
        def manifest = getEntryManifest()
        manifest.edited = deleted
        manifest.writeTo(new FileOutputStream(getManifestFile()))
        // FIXME: move content and enclosures into ENTRY_DELETED_DIR?
        depot.onEntryModified(this)
        // TODO: how to "410 Gone" for enclosures..?
    }

    //==== TODO: in separate FileDepotAtomIndexer? ====

    void generateAtomEntryContent() {
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
        atomEntry.setTitle(getId().toString())
        atomEntry.setSummary(getId().toString())

        def selfUriPath = depot.uriStrategy.makeNegotiatedUriPath(
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

        atomEntry.writeTo(new FileOutputStream(getAtomEntryFile()))
    }

    protected File getAtomEntryFile() {
        return newContentFile(ATOM_ENTRY_MEDIA_TYPE)
    }

    Entry getParsedAtomEntry() {
        def entryFile = getAtomEntryFile()
        // TODO: or entryFile olderThan .. manifestFile?
        if (!entryFile.isFile()
            || entryFile.lastModified() < getManifestFile().lastModified()) {
            generateAtomEntryContent()
        }
        return Abdera.instance.parser.parse(new FileInputStream(entryFile)).root
    }

}
