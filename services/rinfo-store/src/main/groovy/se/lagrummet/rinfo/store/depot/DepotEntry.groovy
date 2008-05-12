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

    private Entry entryManifest

    DepotEntry(depot, entryDir, knownUriPath=null) {
        this.depot = depot
        this.entryDir = entryDir
        this.entryUriPath = knownUriPath
        entryContentDir = new File(entryDir, ENTRY_CONTENT_DIR_NAME)
        // TODO: reintroduce EntryNotFoundException on !entryContentDir.isDir()?
        //  .. and/or EntryDeletedException?
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
        def uri = new URI(getId())
        assert depot.withinBaseUri(uri)
        return uri.path
    }

    String getId() {
        return getEntryManifest().id.toString()
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
        if (!entryManifest) {
            entryManifest = Abdera.instance.parser.parse(
                    new FileInputStream(getManifestFile())).root
        }
        return entryManifest
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
    // enclosures? Or do as user of filedepot?

    void update(timestamp, contents=null, enclosures=null) {
        depot.handleEntryModification(this)
    }

    void delete(timestamp) {
        // TODO: move content and enclosures into ENTRY_DELETED_DIR?
        depot.handleEntryModification(this)
    }

    void generateAtomEntryContent() {
        /* TODO:
            - how tombstones are represented!
        */
        def entry = Abdera.instance.newEntry()
        // TODO: getEntryManifest().clone() ?
        entry.id = getId()
        def publDate = getPublished()
        if (publDate) {
            entry.published = publDate
        }
        entry.updated = getUpdated()

        // TODO: what to use as values?
        entry.setTitle(getId())
        entry.setSummary(getId())

        def selfUriPath = depot.uriStrategy.makeNegotiatedUriPath(
                getEntryUriPath(), ATOM_ENTRY_MEDIA_TYPE)
        entry.addLink(selfUriPath, "self")

        // TODO: add md5 (link extension) or sha (xml-dsig)?
        def contentIsSet = false
        for (content in findContents()) {
            if (content.mediaType == ATOM_ENTRY_MEDIA_TYPE) {
                continue
            }
            if (!contentIsSet && content.mediaType == null) {
                // FIXME: which mediaType()+lang) is content?
                entry.setContent(new IRI(content.depotUriPath), content.mediaType)
                contentIsSet = true
            } else {
                entry.addLink(content.depotUriPath,
                        "alternate",
                        content.mediaType,
                        null, // title
                        content.lang,
                        content.file.length())
            }
        }
        for (enclContent in findEnclosures()) {
            entry.addLink(enclContent.depotUriPath,
                    "enclosure",
                    enclContent.mediaType,
                    null, // title
                    enclContent.lang,
                    enclContent.file.length())
        }
        entry.writeTo(new FileOutputStream(getAtomEntryFile()))
    }

    protected File getAtomEntryFile() {
        def atomHint = depot.uriStrategy.hintForMediaType(
                ATOM_ENTRY_MEDIA_TYPE)
        def atomEntryFileName = "content." + atomHint
        return new File(entryContentDir, atomEntryFileName)
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
