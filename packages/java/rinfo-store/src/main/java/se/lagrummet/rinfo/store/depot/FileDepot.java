package se.lagrummet.rinfo.store.depot;

import java.util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;


public class FileDepot implements Depot {

    //private final Logger logger = LoggerFactory.getLogger(FileDepot.class);

    protected File baseDir;
    protected URI baseUri;
    protected PathHandler pathHandler;
    protected Atomizer atomizer;
    protected FileDepotBackend backend;

    private boolean initialized = false;

    public FileDepot() {
        pathHandler = new DefaultPathHandler();
        atomizer = new Atomizer(this);
        backend = new FileDepotBackend(this);
    }

    public FileDepot(URI baseUri, File baseDir)
            throws FileNotFoundException {
        this();
        this.baseDir = baseDir;
        this.baseUri = baseUri;
        // TODO:? .. or require manual call, or run at first openSession()?
        initialize();
    }

    public File getBaseDir() { return baseDir; }
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public URI getBaseUri() { return baseUri; }
    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public PathHandler getPathHandler() { return pathHandler; }
    public void setPathHandler(PathHandler pathHandler) {
        this.pathHandler = pathHandler;
    }

    public Atomizer getAtomizer() { return atomizer; }


    public void initialize() throws FileNotFoundException {
        if (baseDir == null) {
            throw new IllegalStateException("The depot baseDir has not been set.");
        }
        if (!getBaseDir().exists()) {
            baseDir.mkdir();
        }
        this.initialized = true;
    }

    public DepotSession openSession() {
        //if (!initialized) { initialize(); }
        return new FileDepotSession(this);
    }


    public List<DepotContent> find(String uriPath) throws DepotReadException {
        //if (indexer.canView(uriPath, params)) {
        //    return indexer.getIndexView(uriPath);
        //}
        if (hasFeedView(uriPath)) {
            DepotContent feed = backend.getFeedContent(uriPath);
            if (feed==null) {
                return null;
            }
            return Arrays.asList(feed);
        }
        return findEntryContents(uriPath);
    }

    public List<DepotContent> findEntryContents(String uriPath)
            throws DepotReadException {
        ParsedPath parsedPath = pathHandler.parseUriPath(uriPath);
        if (parsedPath==null || parsedPath.equals("")) {
            return null;
        }
        return findEntryContents(parsedPath);
    }

    public List<DepotContent> findEntryContents(ParsedPath path)
            throws DepotReadException {
        List<DepotContent> results = new ArrayList<DepotContent>();
        DepotEntry depotEntry = getEntry(path.getDepotUriPath());
        if (depotEntry != null) {
            String mediaType = null;
            String mediaHint = path.getMediaHint();
            if (mediaHint != null) {
                mediaType = pathHandler.mediaTypeForHint(mediaHint);
            }
            results = depotEntry.findContents(mediaType, path.getLang());
        } else { // enclosure..
            DepotContent content = getContent(path.getDepotUriPath());
            if (content != null) {
                results.add(content);
            }
        }
        return results;
    }

    public DepotContent getContent(String uriPath) {
        return backend.getContent(uriPath);
    }

    public DepotEntry getEntry(URI entryUri) throws DepotReadException {
        assertWithinBaseUri(entryUri);
        return getEntry(entryUri.getPath());
    }

    public DepotEntry getEntry(String uriPath) throws DepotReadException {
        DepotEntry depotEntry = backend.getUncheckedDepotEntry(uriPath);
        if (depotEntry != null) {
            depotEntry.assertIsNotDeleted();
            depotEntry.assertIsNotLocked();
        }
        return depotEntry;
    }

    public boolean hasEntry(URI entryUri) {
        assertWithinBaseUri(entryUri);
        return hasEntry(entryUri.getPath());
    }

    public boolean hasEntry(String uriPath) {
        return backend.hasEntry(uriPath);
    }


    public boolean hasFeedView(String uriPath) {
        return uriPath.startsWith(atomizer.getFeedPath());
    }


    public Iterator<DepotEntry> iterateEntries() {
        return iterateEntries(false, false);
    }

    public Iterator<DepotEntry> iterateEntries(boolean includeHistorical) {
        return iterateEntries(includeHistorical, false);
    }

    public Iterator<DepotEntry> iterateEntries(
            boolean includeHistorical, boolean includeDeleted) {
        return backend.iterateEntries(
                includeHistorical, includeDeleted);
    }


    /* TODO: checkConsistency() ?
        - ensures no locks (in entries)
        - atomizer.checkConsistency()
            - ensures no locks (in feed dir)
            - ensures feeds chain as expected
            - opt. ensures all entries are properly indexed
    */


    boolean withinBaseUri(URI uri) {
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


}
