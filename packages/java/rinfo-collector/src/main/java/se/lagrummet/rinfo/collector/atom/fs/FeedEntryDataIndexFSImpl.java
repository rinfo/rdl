package se.lagrummet.rinfo.collector.atom.fs;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.AtomDate;

import se.lagrummet.rinfo.collector.atom.FeedEntryDataIndex;


public class FeedEntryDataIndexFSImpl implements FeedEntryDataIndex {

    private File indexDir;
    private static final String ENCODING = "UTF-8";

    public FeedEntryDataIndexFSImpl(File indexDir) {
        this.indexDir = indexDir;
    }

    public File getIndexDir() { return indexDir; }
    public void setIndexDir(File indexDir) {
        this.indexDir = indexDir;
    }

    public Map<IRI, AtomDate> getEntryDataForCompleteFeedId(IRI feedId) {
        File feedIndexFile = getFeedIndexFile(feedId);
        Scanner scanner = null;
        try {
            scanner = new Scanner(feedIndexFile, ENCODING);
        } catch (FileNotFoundException e) {
            return null;
        }
        Map<IRI, AtomDate> entryData = new LinkedHashMap<IRI, AtomDate>();
        try {
            while (scanner.hasNextLine()) {
                String row = scanner.nextLine();
                int i = row.indexOf("\t");
                entryData.put(new IRI(row.substring(0, i)),
                        new AtomDate(row.substring(i + 1)));
            }
        } finally {
            scanner.close();
        }

        return entryData;
    }

    public void storeEntryDataForCompleteFeedId(IRI feedId, Map<IRI, AtomDate> entryData) {
        Index index = null;
        try {
            index = getFeedIndex(feedId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            for (Map.Entry<IRI, AtomDate> item : entryData.entrySet()) {
                index.storeEntryData(item.getKey(), item.getValue(), false);
            }
        } finally {
            index.close();
        }
    }

    public Index getFeedIndex(IRI feedId) throws IOException {
        return new Index(feedId);
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

    public class Index {
        PrintWriter pw;
        public Index(IRI feedId) throws IOException {
            File indexFile = getFeedIndexFile(feedId);
            indexFile.createNewFile();
            pw = new PrintWriter(indexFile, ENCODING);
        }
        public void storeEntryData(IRI entryId, AtomDate updated) {
            storeEntryData(entryId, updated, true);
        }
        void storeEntryData(IRI entryId, AtomDate updated, boolean flush) {
            pw.println(entryId.toString() + "\t" + updated.toString());
            if (flush) {
                pw.flush();
            }
        }
        public void close() {
            pw.close();
        }
    }

}
