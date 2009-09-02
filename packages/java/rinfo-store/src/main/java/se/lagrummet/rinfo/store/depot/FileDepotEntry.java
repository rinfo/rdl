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

    protected static IOFileFilter NON_ENTRY_DIR_FILTER =  new AbstractFileFilter() {
        public boolean accept(File it) {
            return !isEntryDir(it.getParentFile()) && !it.isHidden();
        }
    };

    protected static FileFilter PUBLIC_FILE_FILTER = new FileFilter() {
        public boolean accept(File it) {
            return !it.isHidden() && !it.getName().equals(ENTRY_CONTENT_DIR_NAME);
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
            //  - receiving forMediaType in findContents
            //  - calling depot.mediaTypeForSuffix that forwards to mediaTypeForHint..
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
     * A generic metadata file for usage specific needs. Can be used e.g.
     * to store additional information about creation source etc.
     */
    public File getMetaFile(String fileName) {
        return new File(getMetaDir(), fileName);
    }


    protected String computeEntryUriPath() throws URISyntaxException {
        URI uri = getId();
        depot.assertWithinBaseUri(uri);
        return uri.getPath();
    }

    protected Entry getEntryManifest() {
        if (manifest==null) {
            try {
                InputStream inStream = new FileInputStream(getManifestFile());
                manifest = (Entry) Abdera.getInstance().getParser().parse(
                        inStream).getRoot();
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

    protected void saveManifest(Entry manifest) throws IOException {
        OutputStream outStream = new FileOutputStream(getManifestFile());
        manifest.writeTo(outStream);
        outStream.close();
    }


    protected DepotContent enclosedDepotContent(File file) {
        String uriPath = toEnclosedUriPath(file);
        String mediaType = depot.computeMediaType(file);
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

    protected File getMetaDir() {
        if (!genericMetaDir.exists()) {
            genericMetaDir.mkdir();
        }
        return genericMetaDir;
    }

    /* TODO:? To call when modified (to re-read props from manifest..)
    protected void reset() { manifest = null; entryUriPath = null; }
    */


    //==== TODO: above in DepotEntryView base class? ====

    public void create(Date createTime, List<SourceContent> sourceContents)
            throws DepotWriteException, IOException {
        create(createTime, sourceContents, null);
    }

    public void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures)
            throws DepotWriteException, IOException {
        create(createTime, sourceContents, sourceEnclosures, true);
    }

    public void create(Date createTime, List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures, boolean releaseLock)
            throws DepotWriteException, IOException {
        if(entryContentDir.exists()) {
            // TODO:? if entry is deleted, spec. that it can be recreated.
            throw new DuplicateDepotEntryException(this);
        }
        entryContentDir.mkdir();
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
        depot.onEntryModified(this);
        if (releaseLock) {
            unlock();
        }
    }

    public void update(Date updateTime,
            List<SourceContent> sourceContents)
            throws DepotWriteException, IOException {
        update(updateTime, sourceContents, null);
    }

    public void update(Date updateTime,
            List<SourceContent> sourceContents,
            List<SourceContent> sourceEnclosures)
            throws DepotWriteException, IOException {
        boolean selfLocked = !isLocked();
        if (selfLocked) {
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

        depot.onEntryModified(this);
        if (selfLocked) {
            unlock();
        }
    }

    public void delete(Date deleteTime)
            throws DeletedDepotEntryException, DepotIndexException,
                   IOException {
        boolean selfLocked = !isLocked();
        if (selfLocked) {
            lock();
        }
        if (isDeleted()) {
            throw new DeletedDepotEntryException(this);
        }
        getDeletedMarkerFile().createNewFile();
        Entry manifest = getEntryManifest();
        manifest.setUpdated(deleteTime);

        // TODO: wipe content and enclosures
        // - but keep generated content.entry? (as meta-file?)
        // .. (opt. move away..? zip?)
        rollOffToHistory();

        saveManifest(manifest);
        depot.onEntryModified(this);
        if (selfLocked) {
            unlock();
        }
    }

    // TODO:IMPROVE: resurrect()? (clears deleted state + (partial) create)


    public void lock() throws IOException {
        getLockedMarkerFile().createNewFile();
    }

    public void unlock() throws IOException {
        getLockedMarkerFile().delete();
    }


    public void rollback() throws DepotWriteException, IOException {
        if (hasHistory()) {
            restorePrevious();
        } else {
            wipeout();
        }
    }

    public boolean hasHistory() {
        return getUpdated().compareTo(getPublished()) > 0 &&
            DatePathUtil.youngestEntryHistoryDir(entryContentDir) != null;
    }

    public void wipeout() throws DepotIndexException, IOException {
        lock();
        rollOffToHistory();
        FileUtils.deleteDirectory(entryContentDir);
        FilePathUtil.removeEmptyTrail(
                entryContentDir.getParentFile(), depot.getBaseDir());
    }

    protected void restorePrevious() throws DepotIndexException, IOException {
        lock();
        File rollOffDir = newRollOffDir();
        rollOffToDir(rollOffDir);
        // TODO:IMPROVE: use a "HistoryEntry" and "update" from that?
        File historyDir = DatePathUtil.youngestEntryHistoryDir(entryContentDir);
        restoreFromRollOff(historyDir);
        FileUtils.deleteDirectory(historyDir);
        FilePathUtil.removeEmptyTrail(historyDir.getParentFile(), entryContentDir);
        FileUtils.deleteDirectory(rollOffDir);
        unlock();
    }


    protected void addContent(SourceContent srcContent)
            throws DepotWriteException, IOException {
        addContent(srcContent, false);
    }

    protected void addContent(SourceContent srcContent, boolean replace)
            throws DepotWriteException, IOException {
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
        String enclPath = enclUriPath.replaceFirst(entryUriPath, "");
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


    protected void rollOffToHistory() throws DepotIndexException, IOException {
        rollOffToDir(newHistoryDir());
    }

    protected void rollOffToDir(File destDir) throws IOException {
        // NOTE: this means existing content not in the update is removed.
        //      - *including enclosures*

        /* TODO:IMPROVE: use an entryContentDir to keep structure (boxed
        content, enclosures) similar to regular entries (for a possible
        HistoryEntry)? But see iterateEntries (it scans for
        entryContentDir name..)! */

        FileUtils.moveFileToDirectory(getManifestFile(), destDir, false);

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

    protected void restoreFromRollOff(File rolledOffDir) throws IOException {
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

    protected File getMovedEnclosuresDir(File destDir) {
        File enclosuresDir = new File(destDir, MOVED_ENCLOSURES_DIR_NAME);
        if (!enclosuresDir.exists()) {
            enclosuresDir.mkdir();
        }
        return enclosuresDir;
    }

}