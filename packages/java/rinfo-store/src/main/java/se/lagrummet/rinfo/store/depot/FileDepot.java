package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;


public class FileDepot {

    private final Logger logger = LoggerFactory.getLogger(FileDepot.class);

    private URI baseUri;
    private File baseDir;
    private String feedPath;
    private Atomizer atomizer;
    // TODO: configure via metadata.. (IoC? Data in baseDir?)
    private UriPathProcessor pathProcessor = new UriPathProcessor();

    public FileDepot() {
        this.atomizer = new Atomizer(this);
    }

    public FileDepot(URI baseUri) {
        this();
        this.baseUri = baseUri;
    }

    public FileDepot(URI baseUri, File baseDir, String feedPath)
            throws ConfigurationException {
        this(baseUri);
        this.setBaseDir(baseDir);
        this.feedPath = feedPath;
    }

    public URI getBaseUri() { return baseUri; }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public File getBaseDir() { return baseDir; }

    public void setBaseDir(File baseDir) throws ConfigurationException {
        this.baseDir = baseDir;
        if (!baseDir.isDirectory()) {
            throw new ConfigurationException(
                    "Directory "+baseDir+" does not exist.");
        }
    }

    public String getFeedPath() { return feedPath; }

    public void setFeedPath(String feedPath) {
        this.feedPath = feedPath;
    }

    public Atomizer getAtomizer() { return atomizer; }

    public UriPathProcessor getPathProcessor() { return pathProcessor; }


    //== Entry and Content Lookups ==

    public List<DepotContent> find(String uriPath) throws DeletedDepotEntryException {
        List results = new ArrayList();

        if (uriPath.startsWith(feedPath)) {
            DepotContent feed = getFeedContent(uriPath);
            if (feed==null) {
                return null;
            }
            return Arrays.asList(feed);
        }

        ParsedPath parsed = pathProcessor.parseUriPath(uriPath);
        if (parsed==null || parsed.equals("")) {
            return null;
        }

        DepotEntry depotEntry = getEntry(parsed.getDepotUriPath());
        if (depotEntry!=null) {
            String mediaType = pathProcessor.mediaTypeForHint(parsed.getMediaHint());
            results = depotEntry.findContents(mediaType, parsed.getLang());
        } else { // enclosure..
            DepotContent content = getContent(parsed.getDepotUriPath());
            if (content!=null) {
                results.add(content);
            }
        }
        return results;
    }

    // TODO: throw EntryNotFoundException if !isEntryDir?
    public DepotEntry getEntry(String uriPath)
            throws DeletedDepotEntryException {
        return getEntry(uriPath, true);
    }

    public DepotEntry getEntry(String uriPath, boolean mustExist)
            throws DeletedDepotEntryException {
        File entryDir = getEntryDir(uriPath);
        if (DepotEntry.isEntryDir(entryDir)) {
            return new DepotEntry(this, entryDir, uriPath, mustExist);
        }
        return null;
    }

    public DepotEntry getEntry(URI entryUri) throws DeletedDepotEntryException {
        return getEntry(entryUri, true);
    }

    public DepotEntry getEntry(URI entryUri, boolean mustExist)
            throws DeletedDepotEntryException {
        assertWithinBaseUri(entryUri);
        return getEntry(entryUri.getPath(), mustExist);
    }

    public DepotContent getContent(String uriPath) {
        File file = new File(baseDir, toFilePath(uriPath));
        if (!file.isFile()) {
            return null;
        }
        String mediaType = computeMediaType(file);
        return new DepotContent(file, uriPath, mediaType);
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


    //== Feed Related ==

    // TODO: set via this or inject configured atomizer?
    //  - don't set via this, use Spring "compound property names":
    //      atomizer.feedBatchSize, atomizer.feedSkeleton

    public int getFeedBatchSize() {
        return atomizer.getFeedBatchSize();
    }
    public void setFeedBatchSize(int batchSize) {
        atomizer.setFeedBatchSize(batchSize);
    }

    public void setFeedSkeleton(String feedSkeleton) throws FileNotFoundException {
        atomizer.setFeedSkeleton(feedSkeleton);
    }

    public DepotContent getFeedContent(String uriPath) {
        // TODO: Require suffix in req? And/or conneg?
        return getContent(uriPath + ".atom");
    }

    public String getSubscriptionPath() {
        // TODO: less hard-coded..
        return feedPath+"/current";
    }

    protected String pathToArchiveFeed(Date youngestDate) {
        String archPath = DatePathUtil.toFeedArchivePath(youngestDate);
        return feedPath+"/"+archPath;
    }

    protected String toFeedFilePath(String uriPath) {
        // TODO: less hard-coded..
        return toFilePath(uriPath) + ".atom";
    }


    //== Path and File Related ==

    public boolean withinBaseUri(URI uri) {
        return uri.getHost().equals(baseUri.getHost()) &&
                uri.getScheme().equals(baseUri.getScheme()) &&
                uri.getPort() == baseUri.getPort();
    }

    void assertWithinBaseUri(URI uri) {
        if (!withinBaseUri(uri)) {
            throw new DepotUriException(
                    "The URI <"+uri+"> is not within <"+baseUri+">.");
        }
    }

    // FIXME: Knows *very* little! Configurable?
    public String computeMediaType(File file) {
        String mtype = URLConnection.getFileNameMap().getContentTypeFor(file.getName());
        // TODO: this is too simple. Unify or only via some fileExtensionUtil..
        if (mtype==null) {
            String[] dotSplit = file.getName().split("\\.");
            try {
                mtype = pathProcessor.mediaTypeForHint( dotSplit[dotSplit.length-1] );
            } catch (UnknownMediaTypeException e) {
                ; // pass
            }
        }
        return mtype;
    }

    protected File getEntryDir(String uriPath) {
        return new File(baseDir, toFilePath(uriPath));
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
            sb.append(URLEncoder.encode(segments[i]));
        }
        return sb.toString();
    }

    protected String toFilePath(URI uri) {
        assertWithinBaseUri(uri);
        return toFilePath(uri.getPath());
    }

    //== Writing Specifics ==

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents)
            throws IOException,
                   DeletedDepotEntryException, DuplicateDepotEntryException {
        return createEntry(entryUri, created, contents, null);
    }

    public DepotEntry createEntry(URI entryUri, Date created,
            List<SourceContent> contents,
            List<SourceContent> enclosures)
            throws IOException,
                   DeletedDepotEntryException, DuplicateDepotEntryException {
        assertWithinBaseUri(entryUri);
        String uriPath = entryUri.getPath();
        File entryDir = getEntryDir(uriPath);
        FileUtils.forceMkdir(entryDir);
        DepotEntry depotEntry = new DepotEntry(this, entryDir, uriPath);
        depotEntry.create(created, contents, enclosures);
        return depotEntry;
    }

    public void onEntryModified(DepotEntry depotEntry) throws IOException {
        atomizer.generateAtomEntryContent(depotEntry);
        // TODO: update latest feed index file (may create new file and modify
        // next-to-last (add next-archive)?)! Since any modifying means
        // a new updated depotEntry in the feeds..
    }

    public void generateIndex() throws IOException {
        atomizer.generateIndex();
    }

    public Collection<DepotEntry> makeEntryBatch() {
        return atomizer.makeEntryBatch();
    }

    public void indexEntries(Collection<DepotEntry> entryBatch) throws IOException {
        atomizer.indexEntries(entryBatch);
    }

    /* TODO: checkConsistency() ?
        - ensures no locks (in entries)
        - atomizer.checkConsistency()
            - ensures no locks (in feed dir)
            - ensures feeds chain as expected
            - opt. ensures all entries are properly indexed
    */

}
