package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.lang.StringUtils;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;


public class FileDepotEntry implements DepotEntry {

    public static final String ENTRY_CONTENT_DIR_NAME = "ENTRY-INFO";
    public static final String MANIFEST_FILE_NAME = "manifest.xml";
    public static final Pattern CONTENT_FILE_PATTERN = Pattern.compile(
            "content(?:-(\\w{2}))?.(\\w+)");
    public static final String DELETED_FILE_NAME = "DELETED";
    public static final String GENERIC_META_DIR_NAME = "local-meta";
    public static final String LOCKED_FILE_NAME = "LOCKED";
    public static final String MOVED_ENCLOSURES_DIR_NAME = "enclosures";
    public static final String ROLLOFF_DIR_NAME = "TEMP_ROLLOFF";

    protected static final IOFileFilter NON_ENTRY_DIR_FILTER =  new AbstractFileFilter() {
        public boolean accept(File it) {
            return !isEntryDir(it.getParentFile()) &&
                !it.isHidden() &&
                !it.getName().startsWith(".");
        }
    };

    protected static final FileFilter PUBLIC_FILE_FILTER = new FileFilter() {
        public boolean accept(File it) {
            return !it.isHidden() &&
                !it.getName().startsWith(".") &&
                !it.getName().equals(ENTRY_CONTENT_DIR_NAME);
        }
    };

    protected File entryDir;
    protected File entryContentDir;
    protected File genericMetaDir;

    private FileDepot depot;

    private Entry manifest;
    private String entryUriPath;


    public FileDepotEntry(FileDepot depot, File entryDir, String knownUriPath)
            throws DeletedDepotEntryException, LockedDepotEntryException {
        initialize(depot, entryDir, knownUriPath);
        assertIsNotDeleted();
        assertIsNotLocked();
    }

    protected FileDepotEntry() { }

    protected void initialize(FileDepot depot, File entryDir, String knownUriPath) {
        this.depot = depot;
        this.entryDir = entryDir;
        this.entryUriPath = knownUriPath;
        entryContentDir = new File(entryDir, ENTRY_CONTENT_DIR_NAME);
        genericMetaDir = new File(entryContentDir, GENERIC_META_DIR_NAME);
    }

    public static DepotEntry newUncheckedDepotEntry(
            FileDepot depot, File entryDir, String knownUriPath) {
        FileDepotEntry depotEntry = new FileDepotEntry();
        depotEntry.initialize(depot, entryDir, knownUriPath);
        return depotEntry;
    }

    public static boolean isEntryDir(File dir) {
        return new File(dir, ENTRY_CONTENT_DIR_NAME).isDirectory();
    }

    public String toString() {
        return "FileDepotEntry(entryDir="+entryDir+")";
    }

    public Depot getDepot() {
        return depot;
    }

    public String getEntryUriPath() {
        if (entryUriPath==null) {
            try {
                entryUriPath = computeEntryUriPath();
            } catch (URISyntaxException e) {
                throw new RuntimeException("Malformed entry URI.", e);
            }
        }
        return entryUriPath;
    }

    public URI getId() {
        try {
            return getEntryManifest().getId().toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Malformed entry URI.", e);
        }
    }

    public Date getPublished() {
        return getEntryManifest().getPublished();
    }

    public Date getUpdated() {
        return getEntryManifest().getUpdated();
    }

    public boolean isDeleted() {
        return getDeletedMarkerFile().isFile();
    }

    public boolean isLocked() {
        return getLockedMarkerFile().exists();
    }

    public String getContentMediaType() {
        return getEntryManifest().getContentMimeType().toString();
    }

    public String getContentLanguage() {
        Content contentElem = getEntryManifest().getContentElement();
        if (contentElem == null || contentElem.getLanguage() == null) {
            return null;
        }
        return contentElem.getLanguage().toString();
    }

    /**
     * The last system modification timestamp of this entry.
     */
    public long lastModified() {
        return getManifestFile().lastModified();
    }


    public void assertIsNotDeleted() throws DeletedDepotEntryException {
        if (isDeleted()) {
            throw new DeletedDepotEntryException(this);
        }
    }

    public void assertIsNotLocked() throws LockedDepotEntryException {
        if (isLocked()) {
            throw new LockedDepotEntryException(this);
        }
    }


    public static Iterator<DepotEntry> iterateEntries(
            final FileDepot depot,
            boolean includeHistorical, final boolean includeDeleted) {

        final Iterator<File> manifestIter = FileUtils.iterateFiles(
                depot.getBaseDir(),
                new NameFileFilter(MANIFEST_FILE_NAME),
                HiddenFileFilter.VISIBLE
            );
        /* TODO:
        if (!includeHistorical) {
            dirFilter in manifestIter must be (as the first "if" below does)
                file.parentFile.name != ENTRY_CONTENT_DIR_NAME
            if we use ENTRY_CONTENT_DIR_NAME in history entries.
            And if we don't, we must change the "first if below" to ever
            fulfil includeHistorical.. See also rollOffToHistory.
        }
        */

        return new Iterator<DepotEntry>() {

            public boolean hasNext() { return manifestIter.hasNext(); }

            public DepotEntry next() {
                while (manifestIter.hasNext()) {
                    File contentDir = manifestIter.next().getParentFile();
                    if (!contentDir.getName().equals(ENTRY_CONTENT_DIR_NAME)) {
                        continue;
                    }
                    DepotEntry depotEntry = FileDepotEntry.newUncheckedDepotEntry(
                        depot, contentDir.getParentFile(), null);
                    if (!includeDeleted && depotEntry.isDeleted()) {
                        continue;
                    }
                    // TODO: includeLocked (and/or onlyLocked?) Fail on locked..
                    return depotEntry;
                }
                throw new NoSuchElementException();
            }

            public void remove() { throw new UnsupportedOperationException(); }
        };
    }


    public List<DepotContent> findContents() {
        return findContents(null, null);
    }

    public List<DepotContent> findContents(String forMediaType) {
        return findContents(forMediaType, null);
    }

    public List<DepotContent> findContents(String forMediaType, String forLang) {
        List<DepotContent> found = new ArrayList<DepotContent>();
        // TODO:IMPROVE: if both qualifiers given, get file with newContentFile?
        for (File file : entryContentDir.listFiles()) {
            Matcher match = CONTENT_FILE_PATTERN.matcher(file.getName());
            if (!match.matches()) {
                continue;
            }
            String mediaHint = match.group(2);
            String mediaType = depot.getPathHandler().mediaTypeForHint(mediaHint);
            if (forMediaType!=null && !mediaType.equals(forMediaType)) {
                continue;
            }
            String lang = match.group(1);
            if ("".equals(lang)) lang = null;
            if (forLang!=null && !forLang.equals(lang)) {
                continue;
            }
            // TODO:IMPROVE: we now decouple suffix and hint logic:
            //  .. although this reintroduces map->remap->back-to-map..!
            //  .. and mediaHint *is* very concise.
            //  - calling computeMediaType that forwards to mediaTypeForHint..
            //  .. could allow mediaHint to be collection-dependent in pathHandler..
            String uriPath = depot.getPathHandler().makeNegotiatedUriPath(
                    getEntryUriPath(), mediaType, lang);
            found.add(new DepotContent(file, uriPath, mediaType, lang));
        }
        return found;
    }

    public List<DepotContent> findEnclosures() {
        List<DepotContent> enclosures = new ArrayList<DepotContent>();
        for (File file : entryDir.listFiles(PUBLIC_FILE_FILTER)) {
            if (file.isFile()) {
                enclosures.add( enclosedDepotContent(file) );
            } else if (file.isDirectory()) {
                for (File subfile : ((Collection<File>)FileUtils.listFiles(file,
                        HiddenFileFilter.VISIBLE,
                        NON_ENTRY_DIR_FILTER))) {
                    enclosures.add( enclosedDepotContent(subfile) );
                }
            }
        }
        return enclosures;
    }

    /**
     * Create a generic metadata resource for usage specific needs. Can be used
     * e.g. to store additional information about creation source.
     */
    public OutputStream getMetaOutputStream(String resourceName)
            throws DepotWriteException {
        if (!genericMetaDir.exists()) {
            if (!genericMetaDir.mkdir()) {
                throw new DepotWriteException(
                        "Cannot create entry meta directory: " + genericMetaDir);
            }
        }
        try {
            return new FileOutputStream(new File(genericMetaDir, resourceName));
        } catch (FileNotFoundException e) {
            throw new DepotWriteException(e);
        }
    }

    /**
     * Get a generic metadata stream if it has been previously created with
     * {@link getMetaOutputStream}.
     * @return An InputStream for the resource, or null if it doesn't exist.
     */
    public InputStream getMetaInputStream(String resourceName)
            throws DepotReadException {
        File metaFile = new File(genericMetaDir, resourceName);
        if (!metaFile.exists()) {
            return null;
        }
        try {
            return new FileInputStream(metaFile);
        } catch (FileNotFoundException e) {
            throw new DepotReadException(e);
        }
    }

    protected String computeEntryUriPath() throws URISyntaxException {
        URI uri = getId();
        //depot.assertWithinBaseUri(uri); // this is already stored..
        return uri.getPath();
    }

    protected Entry getEntryManifest() {
        if (manifest==null) {
            try {
                InputStream inStream = new FileInputStream(getManifestFile());
                manifest = (Entry) Abdera.getInstance().getParser().parse(
                        inStream).getRoot();
                manifest.complete();
                inStream.close();
            } catch (Exception e) {
                throw new RuntimeException("Error reading manifest file.", e);
            }
        }
        return manifest;
    }

    protected File getManifestFile() {
        return new File(entryContentDir, MANIFEST_FILE_NAME);
    }

    protected void saveManifest(Entry manifest) throws DepotWriteException {
        try {
            OutputStream outStream = new FileOutputStream(getManifestFile());
            try {
                manifest.writeTo(outStream);
            } finally {
                outStream.close();
            }
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }


    protected DepotContent enclosedDepotContent(File file) {
        String uriPath = toEnclosedUriPath(file);
        String mediaType = depot.getPathHandler().computeMediaType(file.getName());
        return new DepotContent(file, uriPath, mediaType);
    }

    protected String toEnclosedUriPath(File file) {
        String pathRemainder = FilePathUtil.toRelativeFilePath(file, entryDir);
        String uriPath = getEntryUriPath() + "/" + pathRemainder;
        return uriPath;
    }


    protected File getDeletedMarkerFile() {
        return new File(entryContentDir, DELETED_FILE_NAME);
    }

    protected File getLockedMarkerFile() {
        return new File(entryContentDir, LOCKED_FILE_NAME);
    }

    /* TODO:? To call when modified (to re-read props from manifest..)
    protected void resetState() { manifest = null; entryUriPath = null; }
    */


    //==== TODO: above in DepotEntryView base class? ====

    public void create(Date createTime, List<SourceContent> sourceContents)
            throws DepotWriteException {
        create(createTime, sourceContents, null);
    }

    public void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures) throws DepotWriteException {
        create(createTime, sourceContents, sourceEnclosures, true);
    }

    public void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures, boolean releaseLock)
            throws DepotWriteException {
        try {
            if(entryContentDir.exists()) {
                // TODO:? if entry is deleted, spec. that it can be recreated.
                throw new DuplicateDepotEntryException(this);
            }
            if (!entryContentDir.mkdir()) {
                throw new DepotWriteException(
                        "Cannot create entry content directory: " + entryContentDir);
            }
            lock();

            Entry manifest = Abdera.getInstance().newEntry();
            // TODO: unify with rest of getId/entryPath stuff!
            manifest.setId(depot.getBaseUri().resolve(getEntryUriPath()).toString());
            manifest.setPublished(createTime);
            manifest.setUpdated(createTime);

            setPrimaryContent(manifest, sourceContents);
            saveManifest(manifest);

            for (SourceContent content : sourceContents) {
                addContent(content);
            }
            if (sourceEnclosures!=null) {
                for (SourceContent encl : sourceEnclosures) {
                    addEnclosure(encl);
                }
            }
            if (releaseLock) {
                unlock();
            }
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }

    }

    public void update(Date updateTime,
            List<SourceContent> sourceContents) throws DepotWriteException {
        update(updateTime, sourceContents, null);
    }

    public void update(Date updateTime,
            List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures) throws DepotWriteException {
        try {
            boolean locallyLocked = !isLocked();
            if (locallyLocked) {
                lock();
            }
            rollOffToHistory();

            if (sourceContents!=null) {
                for (SourceContent content : sourceContents) {
                    addContent(content, true);
                }
            }
            if (sourceEnclosures!=null) {
                for (SourceContent encl : sourceEnclosures) {
                    addEnclosure(encl, true);
                }
            }

            Entry manifest = getEntryManifest();
            manifest.setUpdated(updateTime);
            setPrimaryContent(manifest, sourceContents);
            saveManifest(manifest);

            if (locallyLocked) {
                unlock();
            }
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }

    public void delete(Date deleteTime)
            throws DeletedDepotEntryException,
                   DepotIndexException, DepotWriteException {
        if (isDeleted()) {
            throw new DeletedDepotEntryException(this);
        }
        boolean locallyLocked = !isLocked();
        if (locallyLocked) {
            lock();
        }
        // TODO:? wipe content and enclosures?
        // - but keep generated content.entry? (as meta-file?)
        // .. (opt. move away..? zip?)
        rollOffToHistory();

        createMarkerFile(getDeletedMarkerFile());

        Entry manifest = getEntryManifest();
        manifest.setUpdated(deleteTime);
        saveManifest(manifest);
        if (locallyLocked) {
            unlock();
        }
    }

    public void lock() throws DepotWriteException {
        createMarkerFile(getLockedMarkerFile());
    }

    public void unlock() throws DepotWriteException {
        removeMarkerFile(getLockedMarkerFile());
    }

    public void resurrect() throws DepotWriteException {
        // TODO:? this removes *all* history. What are the requirements?
        // Should delete wipe history immediately?
        wipeout();
        /*
        // NOTE: this is a variant for keeping history (but incomplete; create
        // will complain since entry path exists..)
        lock();
        File historyDir = rollOffToHistory();
        try {
            FileUtils.moveFileToDirectory(getDeletedMarkerFile(), historyDir, false);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
        unlock();
        */
    }


    // IMPORTANT: these modify *in place*, thus any indexing must occur after!

    public void rollback() throws DepotWriteException {
        if (hasHistory()) {
            restorePrevious();
        } else {
            wipeout();
        }
    }

    protected boolean hasHistory() {
        return getUpdated().compareTo(getPublished()) > 0 &&
            DatePathUtil.youngestEntryHistoryDir(entryContentDir) != null;
    }


    protected void wipeout() throws DepotWriteException {
        try {
            FileUtils.deleteDirectory(entryContentDir);
            FilePathUtil.removeEmptyTrail(
                    entryContentDir.getParentFile(), depot.getBaseDir());
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }

    protected void restorePrevious()
            throws DepotWriteException, DepotIndexException {
        try {
            if (!isLocked())
                lock();
            File rollOffDir = newRollOffDir();
            rollOffToDir(rollOffDir);
            // TODO:IMPROVE: use a "HistoryEntry" and "update" from that?
            File historyDir = DatePathUtil.youngestEntryHistoryDir(
                    entryContentDir);
            restoreFromRollOff(historyDir);
            FileUtils.deleteDirectory(historyDir);
            FilePathUtil.removeEmptyTrail(
                    historyDir.getParentFile(), entryContentDir);
            FileUtils.deleteDirectory(rollOffDir);
            unlock();
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }

    protected void addContent(SourceContent srcContent)
            throws DepotWriteException, SourceCheckException, IOException {
        addContent(srcContent, false);
    }

    protected void addContent(SourceContent srcContent, boolean replace)
            throws DepotWriteException, SourceCheckException, IOException {
        File file = newContentFile(srcContent.getMediaType(), srcContent.getLang());
        if (!replace) {
            if (file.exists()) {
                throw new DuplicateDepotContentException(this, srcContent);
            }
        }
        srcContent.writeTo(file);
    }


    protected void addEnclosure(SourceContent srcContent)
            throws DepotWriteException, IOException {
        addEnclosure(srcContent, false);
    }

    protected void addEnclosure(SourceContent srcContent, boolean replace)
            throws DepotWriteException, IOException {
        String enclUriPath = srcContent.getEnclosedUriPath();
        if (enclUriPath.startsWith("/")) {
            if (!enclUriPath.startsWith(entryUriPath)) {
                throw new DepotUriException(
                        "Enclosure <"+enclUriPath +
                        "> is not within depot entry <"+entryUriPath+">.");
            }
        } else {
            String sep = entryUriPath.endsWith("/") ? "" : "/";
            enclUriPath = entryUriPath + sep + enclUriPath;
        }
        String enclPath = StringUtils.removeStart(enclUriPath, entryUriPath);
        File file = new File(entryDir, enclPath);
        if (!replace) {
            if (file.exists()) {
                throw new DuplicateDepotContentException(this, srcContent);
            }
        }
        FilePathUtil.plowParentDirPath(file);
        srcContent.writeTo(file);
    }


    protected File newContentFile(String mediaType) {
        return newContentFile(mediaType, null);
    }

    protected File newContentFile(String mediaType, String lang) {
        String filename = "content";
        if (lang!=null) {
            filename += "-" + lang;
        }
        String suffix = depot.getPathHandler().hintForMediaType(mediaType);
        filename += "." + suffix;
        return new File(entryContentDir, filename);
    }

    protected void setPrimaryContent(Entry manifest,
            List<SourceContent> sourceContents) {
        if (sourceContents != null && sourceContents.size() > 0) {
            SourceContent content = sourceContents.get(0);
            manifest.setContent(((String)null), content.getMediaType());
            if (content.getLang()!=null && !content.getLang().equals("")) {
                manifest.getContentElement().setLanguage(content.getLang());
            }
        }
    }


    protected File rollOffToHistory() throws DepotWriteException {
        try {
            File historyDir = newHistoryDir();
            rollOffToDir(historyDir);
            return historyDir;
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }

    /**
     * Moves the current content (including manifest and enclosures) to the
     * given directory.
     */
    protected void rollOffToDir(File destDir)
            throws DepotWriteException, IOException {
        // NOTE: this means existing content not in the update is removed.
        //      - *including enclosures*

        /* TODO:IMPROVE: use an entryContentDir to keep structure (boxed
        content, enclosures) similar to regular entries (for a possible
        HistoryEntry)? But see iterateEntries (it scans for
        entryContentDir name..)! */

        File manifestFile = getManifestFile();
        // NOTE: not required to exist, since this may be invoked as part of a
        // rollback. If there is an ongoing update, the manifest file may have
        // moved to a historyDir.
        if (manifestFile.exists()) {
            FileUtils.moveFileToDirectory(manifestFile, destDir, false);
        }

        for (DepotContent content : findContents()) {
            FileUtils.moveToDirectory(content.getFile(), destDir, false);
        }
        if (genericMetaDir.exists()) {
            FileUtils.moveToDirectory(genericMetaDir, destDir, false);
        }
        File enclosuresDir = getMovedEnclosuresDir(destDir);
        for (File file : entryDir.listFiles(PUBLIC_FILE_FILTER)) {
            if (file.isDirectory() && containsSubEntry(file)) {
                /* TODO: This doesn't roll of *any* path leading to a
                sub-entry! Is that ok? I believe so (alt. is forking..)..
                .. although an entry can then "shadow" old enclosures..
                .. We could assert "clean path" in create? */
                continue;
            }
            FileUtils.moveToDirectory(file, enclosuresDir, false);
            // TODO:IMPROVE: removeEmptyTrail(file.getParentFile())?
        }
    }

    protected boolean containsSubEntry(File dir) {
        boolean foundSubEntry = false;
        for (File child : dir.listFiles(PUBLIC_FILE_FILTER)) {
            if (foundSubEntry || isEntryDir(child)) {
                return true;
            } else if (child.isDirectory()) {
                foundSubEntry = containsSubEntry(child);
            }
        }
        return foundSubEntry;
    }

    protected void restoreFromRollOff(File rolledOffDir) throws DepotWriteException,
            IOException {
        FileUtils.moveFileToDirectory(new File(rolledOffDir, MANIFEST_FILE_NAME),
                entryContentDir, false);

        for (File file : rolledOffDir.listFiles()) {
            Matcher match = CONTENT_FILE_PATTERN.matcher(file.getName());
            if (match.matches()) {
                FileUtils.moveFileToDirectory(file, entryContentDir, false);
            }
        }

        File movedMetaDir = new File(rolledOffDir, GENERIC_META_DIR_NAME);
        if (movedMetaDir.exists()) {
            FileUtils.moveDirectoryToDirectory(
                    movedMetaDir, entryContentDir, false);
        }

        File movedEnclDir = getMovedEnclosuresDir(rolledOffDir);
        for (File enclFile : ((Collection<File>)FileUtils.listFiles(movedEnclDir,
                HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE))) {
            String enclPath = FilePathUtil.toRelativeFilePath(
                    enclFile, movedEnclDir);
            File target = new File(entryDir, enclPath);
            FilePathUtil.plowParentDirPath(target);
            FileUtils.moveFile(enclFile, target);
        }
    }

    protected File newHistoryDir() throws DepotIndexException, IOException {
        String dirPath = DatePathUtil.toEntryHistoryPath(getUpdated());
        File dir = new File(entryContentDir, dirPath);
        if (dir.exists()) {
            throw new DepotIndexException(
                    "Cannot create history for depot entry <"+this.getId() +
                    "> at date ["+getUpdated()+"]. File <"+dir+"> is in the way!"
                );
        }
        FileUtils.forceMkdir(dir);
        return dir;
    }

    protected File newRollOffDir() throws DepotIndexException, IOException {
        File dir = new File(entryContentDir, ROLLOFF_DIR_NAME);
        if (dir.exists()) {
            throw new DepotIndexException(
                    "Cannot create roll-off for depot entry <"+this.getId() +
                    "> at date ["+getUpdated()+"]. File <"+dir+"> is in the way!"
                );
        }
        FileUtils.forceMkdir(dir);
        return dir;
    }

    protected File getMovedEnclosuresDir(File destDir) throws DepotWriteException {
        File enclosuresDir = new File(destDir, MOVED_ENCLOSURES_DIR_NAME);
        if (!enclosuresDir.exists()) {
            if (!enclosuresDir.mkdir()) {
                throw new DepotWriteException(
                        "Cannot create moved enclosure directory: " + enclosuresDir);
            }
        }
        return enclosuresDir;
    }

    protected void createMarkerFile(File markerFile) throws DepotWriteException {
        try {
            if (!markerFile.createNewFile()) {
                throw new DepotWriteException(
                        "Cannot create entry marker file " + markerFile);
            }
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
    }

    protected void removeMarkerFile(File markerFile) throws DepotWriteException {
        if (!markerFile.delete()) {
            throw new DepotWriteException(
                    "Cannot remove entry marker file " + markerFile);
        }
    }

}
