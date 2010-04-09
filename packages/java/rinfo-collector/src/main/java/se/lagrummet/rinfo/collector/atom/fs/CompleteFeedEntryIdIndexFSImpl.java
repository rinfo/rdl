package se.lagrummet.rinfo.collector.atom.fs;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;

import org.apache.abdera.i18n.iri.IRI;

import se.lagrummet.rinfo.collector.atom.CompleteFeedEntryIdIndex;


public class CompleteFeedEntryIdIndexFSImpl implements CompleteFeedEntryIdIndex {

    private File indexDir;
    private static final String ENCODING = "UTF-8";

    public CompleteFeedEntryIdIndexFSImpl(File indexDir) {
        this.indexDir = indexDir;
    }

    public File getIndexDir() { return indexDir; }
    public void setIndexDir(File indexDir) {
        this.indexDir = indexDir;
    }

    public Set<IRI> getEntryIdsForCompleteFeedId(IRI feedId) {
        File feedIndexFile = getFeedIndexFile(feedId);
        Scanner scanner = null;
        try {
            scanner = new Scanner(feedIndexFile, ENCODING);
        } catch (FileNotFoundException e) {
            return null;
        }
        Set<IRI> entryIds = new HashSet<IRI>();
        try {
            while (scanner.hasNextLine()) {
                entryIds.add(new IRI(scanner.nextLine()));
            }
        } finally {
            scanner.close();
        }

        return entryIds;
    }

    public void storeEntryIdsForCompleteFeedId(IRI feedId, Set<IRI> entryIds) {
        PrintWriter pw = null;
        try {
            File feedIndexFile = getFeedIndexFile(feedId);
            feedIndexFile.createNewFile();
            pw = new PrintWriter(feedIndexFile, ENCODING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            for (IRI entryId : entryIds) {
                pw.println(entryId.toString());
            }
        } finally {
            pw.close();
        }
    }

    protected File getFeedIndexFile(IRI feedId) {
        String feedPath = toFilePath(feedId);
        return new File(indexDir, feedPath);
    }

    protected String toFilePath(IRI feedId) {
        try {
            return URLEncoder.encode(feedId.toString(), ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
