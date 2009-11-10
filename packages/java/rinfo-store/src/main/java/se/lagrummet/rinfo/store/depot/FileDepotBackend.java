package se.lagrummet.rinfo.store.depot;

import java.util.Iterator;
import java.util.Date;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;


public class FileDepotBackend {

    private FileDepot depot;
    private String indexSuffix;

    public FileDepotBackend(FileDepot depot) {
        this.depot = depot;
        this.indexSuffix = ".atom";
    }

    public boolean hasEntry(String uriPath) {
        File entryDir = getEntryDir(uriPath);
        return FileDepotEntry.isEntryDir(entryDir);
    }

    public DepotEntry getUncheckedDepotEntry(String uriPath) {
        File entryDir = getEntryDir(uriPath);
        if (!FileDepotEntry.isEntryDir(entryDir)) {
            return null;
        }
        return FileDepotEntry.newUncheckedDepotEntry(depot, entryDir, uriPath);
    }

    public DepotEntry newBlankEntry(String uriPath)
            throws DepotReadException, DepotWriteException {
        File entryDir = getEntryDir(uriPath);
        try {
            FileUtils.forceMkdir(entryDir);
        } catch (IOException e) {
            throw new DepotWriteException(e);
        }
        return new FileDepotEntry(depot, entryDir, uriPath);
    }

    public DepotContent getContent(String uriPath) {
        File file = new File(depot.getBaseDir(), toFilePath(uriPath));
        if (!file.isFile()) {
            return null;
        }
        String mediaType = depot.getPathHandler().computeMediaType(file.getName());
        return new DepotContent(file, uriPath, mediaType);
    }

    public Iterator<DepotEntry> iterateEntries(
            boolean includeHistorical, boolean includeDeleted) {
        return FileDepotEntry.iterateEntries(
                depot, includeHistorical, includeDeleted);
    }

    // TODO:IMPROVE: don't hard-code ".atom" (or don't even do it at all?)
    // Most importantly, DepotContent for a feed now has a non-working uriPath!
    // I.e. we must consider that public feed uri:s are non-suffixed (currently)..
    // This should reasonably be stiched together with pathHandler..
    // TODO: *or* simply conneg on suffix (for all "plain" content)!
    public DepotContent getFeedContent(String uriPath) {
        // TODO:? Internal repr + cOnneg?
        if (!uriPath.endsWith(indexSuffix)) {
            uriPath += indexSuffix;
        }
        return getContent(uriPath);
    }

    protected File getEntryDir(String uriPath) {
        return new File(depot.getBaseDir(), toFilePath(uriPath));
    }

    // TODO: rename to getIndexFile
    protected File getFeedFile(String uriPath) {
        return new File(depot.getBaseDir(), toFilePath(uriPath) + indexSuffix);
    }

    protected String toFilePath(String uriPath) {
        if (!uriPath.startsWith("/")) {
            throw new DepotUriException(
                    "URI path must be absolute and not full. Was: " + uriPath);
        }
        String localUriPath = StringUtils.removeStart(uriPath, "/");

        // TODO: do a cleaner (even reversable?) algorithm!
        // perhaps use configured PathHandler, FilePathUtil or complement thereof?
        String path = localUriPath.replace(":", "/_3A_");

        String[] segments = path.split("/");
        StringBuffer sb = new StringBuffer();
        String filePathSep = File.separator;
        for (int i=0; i<segments.length; i++) {
            if (i!=0) sb.append(filePathSep);
            try {
                sb.append(URLEncoder.encode(segments[i], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new DepotUriException(e);
            }
        }
        return sb.toString();
    }

}
