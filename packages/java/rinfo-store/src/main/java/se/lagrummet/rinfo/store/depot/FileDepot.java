package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;


public class FileDepot implements Depot {

    private final Logger logger = LoggerFactory.getLogger(FileDepot.class);

    private URI baseUri;
    private File baseDir;
    private String feedPath;
    private Atomizer atomizer;
    private PathHandler pathHandler = new PathHandler();

    public FileDepot() {
        this.atomizer = new Atomizer(this);
    }

    public FileDepot(URI baseUri, File baseDir, String feedPath)
            throws ConfigurationException, FileNotFoundException {
        this();
        this.baseUri = baseUri;
        this.setBaseDir(baseDir);
        this.feedPath = feedPath;
    }

    public URI getBaseUri() { return baseUri; }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public File getBaseDir() {
        if (baseDir == null) {
            throw new IllegalStateException("FileDepot.baseDir is null.");
        }
        return baseDir;
    }

    public void setBaseDir(File baseDir) throws FileNotFoundException {
        this.baseDir = baseDir;
        // TODO:? move to initialize and require depot to be initialized before use?
        if (!baseDir.exists()) {
            baseDir.mkdir();
        }
    }

    public String getFeedPath() { return feedPath; }

    public void setFeedPath(String feedPath) {
        this.feedPath = feedPath;
    }

    public Atomizer getAtomizer() { return atomizer; }

    public PathHandler getPathHandler() { return pathHandler; }


    //== Entry and Content Lookups ==

    public List<DepotContent> find(String uriPath) throws DepotReadException {
        List<DepotContent> results = new ArrayList<DepotContent>();

        if (uriPath.startsWith(feedPath)) {
            DepotContent feed = getFeedContent(uriPath);
            if (feed==null) {
                return null;
            }
            return Arrays.asList(feed);
        }

        ParsedPath parsed = pathHandler.parseUriPath(uriPath);
        if (parsed==null || parsed.equals("")) {
            return null;
        }

        DepotEntry depotEntry = getEntry(parsed.getDepotUriPath());
        if (depotEntry!=null) {
            String mediaType = null;
            String mediaHint = parsed.getMediaHint();
            if (mediaHint != null) {
                mediaType = pathHandler.mediaTypeForHint(mediaHint);
            }
            results = depotEntry.findContents(mediaType, parsed.getLang());
        } else { // enclosure..
            DepotContent content = getContent(parsed.getDepotUriPath());
            if (content!=null) {
                results.add(content);
            }
        }
        return results;
    }


    public DepotEntry getEntry(URI entryUri) throws DepotReadException {
        assertWithinBaseUri(entryUri);
        return getEntry(entryUri.getPath());
    }

    public DepotEntry getEntry(String uriPath) throws DepotReadException {
        DepotEntry depotEntry = getUncheckedDepotEntry(uriPath);
        if (depotEntry != null) {
            depotEntry.assertIsNotDeleted();
            depotEntry.assertIsNotLocked();
        }
        return depotEntry;
    }

    protected DepotEntry getUncheckedDepotEntry(String uriPath) {
        File entryDir = getEntryDir(uriPath);
        if (!DepotEntry.isEntryDir(entryDir)) {
            return null;
        }
        return DepotEntry.newUncheckedDepotEntry(this, entryDir, uriPath);
    }

    public Iterator<DepotEntry> iterateEntries() {
        return iterateEntries(false, false);
    }

    public Iterator<DepotEntry> iterateEntries(boolean includeHistorical) {
        return iterateEntries(includeHistorical, false);
    }

    public Iterator<DepotEntry> iterateEntries(
            boolean includeHistorical, boolean includeDeleted) {
        return DepotEntry.iterateEntries(this, includeHistorical, includeDeleted);
    }


    public DepotContent getContent(String uriPath) {
        File file = new File(getBaseDir(), toFilePath(uriPath));
        if (!file.isFile()) {
            return null;
        }
        String mediaType = computeMediaType(file);
        return new DepotContent(file, uriPath, mediaType);
    }


    //== Feed Related ==

    // TODO:IMPROVE: don't hard-code ".atom" (or don't even do it at all?)
    // Most importantly, DepotContent for a feed now has a non-working uriPath!
    // I.e. we must consider that public feed uri:s are non-suffixed (currently)..
    // This should reasonably be stiched together with pathHandler..
    // TODO: *or* simply conneg on suffix (for all "plain" content)!

    protected DepotContent getFeedContent(String uriPath) {
        // TODO: Require suffix in req? And/or conneg?
        return getContent(uriPath + ".atom");
    }

    protected File getFeedFile(String uriPath) {
        return new File(getBaseDir(), toFilePath(uriPath) + ".atom");
    }

    protected String getSubscriptionPath() {
        // TODO: configurable? Settable "feedSubscriptionSegment"?
        return feedPath+"/current";
    }

    protected String pathToArchiveFeed(Date youngestDate) {
        String archPath = DatePathUtil.toFeedArchivePath(youngestDate);
        return feedPath+"/"+archPath;
    }


    //== Path and File Related ==

    public boolean withinBaseUri(URI uri) {
        if (uri == null || baseUri == null)
            return false;
        return uri.getHost().equals(baseUri.getHost()) &&
                uri.getScheme().equals(baseUri.getScheme()) &&
                uri.getPort() == baseUri.getPort();
    }

    void assertWithinBaseUri(URI uri) {
        if (!withinBaseUri(uri)) {
            throw new DepotUriException(
                    "The URI <"+uri+"> is not within base URI <"+baseUri+">.");
        }
    }

    // FIXME: Knows *very* little! Configurable?
    public String computeMediaType(File file) {
        // TODO: System.setProperty("content.types.user.table",
        // configuredContentTypesPath"), then store the FileNameMap..
        String mtype = URLConnection.getFileNameMap().getContentTypeFor(file.getName());
        // TODO: this is too simple. Unify or only via some fileExtensionUtil..
        if (mtype==null) {
            String[] dotSplit = file.getName().split("\\.");
            try {
                mtype = pathHandler.mediaTypeForHint( dotSplit[dotSplit.length-1] );
            } catch (UnknownMediaTypeException e) {
                ; // pass
            }
        }
        return mtype;
    }

    protected File getEntryDir(String uriPath) {
        return new File(getBaseDir(), toFilePath(uriPath));
    }

    protected String toFilePath(String uriPath) {
        if (!uriPath.startsWith("/")) {
            throw new DepotUriException(
                    "URI path must be absolute and not full. Was: " + uriPath);
        }

        String localUriPath = uriPath.replaceFirst("/", "");

        // FIXME: do a smarter (probably reversable) algorithm!
        String path = localUriPath.replace(":", "/_3A_");

        String[] segments = path.split("/");
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<segments.length; i++) {
            if (i!=0) sb.append("/");
            try {
                sb.append(URLEncoder.encode(segments[i], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return sb.toString();
    }

    protected String toFilePath(URI uri) {
        assertWithinBaseUri(uri);
        return toFilePath(uri.getPath());
    }

    //== Write Specifics ==

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents)
            throws DepotReadException, DepotWriteException,
                   IOException {
        return createEntry(entryUri, created, contents, null);
    }

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            boolean releaseLock)
            throws DepotReadException, DepotWriteException,
                   IOException {
        return createEntry(entryUri, created, contents, null, releaseLock);
    }

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures)
            throws DepotReadException, DepotWriteException,
                   IOException {
        return createEntry(entryUri, created, contents, enclosures, true);
    }

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures,
            boolean releaseLock)
            throws DepotReadException, DepotWriteException,
                   IOException {
        assertWithinBaseUri(entryUri);
        String uriPath = entryUri.getPath();
        File entryDir = getEntryDir(uriPath);
        FileUtils.forceMkdir(entryDir);
        DepotEntry depotEntry = new DepotEntry(this, entryDir, uriPath);
        depotEntry.create(created, contents, enclosures, releaseLock);
        return depotEntry;
    }

    public void onEntryModified(DepotEntry depotEntry) throws IOException {
        atomizer.generateAtomEntryContent(depotEntry);
        // TODO:? update latest feed index file (may create new file and modify
        // next-to-last (add next-archive)?)! Since any modifying means
        // a new updated depotEntry in the feeds..
    }

    public void generateIndex() throws DepotWriteException, IOException {
        File feedDir = new File(getBaseDir(), getFeedPath());
        if (!feedDir.exists()) {
            feedDir.mkdir();
        }
        DepotEntryBatch entryBatch = makeEntryBatch();

        for (Iterator<DepotEntry> iter = iterateEntries(
                atomizer.getIncludeHistorical(), atomizer.getIncludeDeleted());
                iter.hasNext(); ) {
            entryBatch.add(iter.next());
        }

        FileUtils.cleanDirectory(feedDir);
        indexEntries(entryBatch);
    }

    public void indexEntries(DepotEntryBatch entryBatch)
        throws DepotWriteException, IOException {
        atomizer.indexEntries(entryBatch);
    }

    /* TODO: checkConsistency() ?
        - ensures no locks (in entries)
        - atomizer.checkConsistency()
            - ensures no locks (in feed dir)
            - ensures feeds chain as expected
            - opt. ensures all entries are properly indexed
    */

    public DepotEntryBatch makeEntryBatch() {
        return new DepotEntryBatch(this);
    }

}
