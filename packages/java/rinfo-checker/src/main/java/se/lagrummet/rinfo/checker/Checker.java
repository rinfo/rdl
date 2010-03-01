package se.lagrummet.rinfo.checker;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.io.FileUtils;

import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import se.lagrummet.rinfo.store.depot.Depot;
import se.lagrummet.rinfo.store.depot.FileDepot;
import se.lagrummet.rinfo.store.depot.SourceContent;
import se.lagrummet.rinfo.main.storage.FeedCollector;
import se.lagrummet.rinfo.main.storage.StorageSession;
import se.lagrummet.rinfo.main.storage.StorageCredentials;
import se.lagrummet.rinfo.main.storage.StorageHandler;
import se.lagrummet.rinfo.main.storage.CollectorLog;


public class Checker {

    Repository logRepo = new SailRepository(new MemoryStore());
    Depot depot;
    File tempDir;

    private List<StorageHandler> handlers;
    public List<StorageHandler> getHandlers() { return handlers; }
    public void setHandlers(List<StorageHandler> handlers) { this.handlers = handlers; }

    private int maxEntries = -1;
    public int getMaxEntries() { return maxEntries; }
    public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }

    public Checker() throws Exception {
        tempDir = createTempDir();
        depot = new FileDepot(new URI("http://rinfo.lagrummet.se"), tempDir);
        depot.getAtomizer().setFeedPath("/feed");
        ((FileDepot) depot).initialize();
    }

    public void shutdown() throws Exception {
        logRepo.shutDown();
        removeTempDir();
    }

    public Repository checkFeed(String feedUrl) throws Exception {
        return checkFeed(new URL(feedUrl));
    }

    public Repository checkFeed(URL feedUrl) throws Exception {
        return checkFeed(feedUrl, false);
    }

    public Repository checkFeed(URL feedUrl, boolean adminSource) throws Exception {
        CollectorLog coLog = new CollectorLog(logRepo);
        LaxStorageSession storageSession = new LaxStorageSession(
                new StorageCredentials(adminSource),
                depot, handlers, coLog);
        storageSession.setMaxEntries(maxEntries);
        FeedCollector collector = new OneFeedCollector(storageSession);
        collector.readFeed(feedUrl);
        collector.shutdown();
        return logRepo;
    }

    protected File createTempDir() {
        int i = 0;
        File tempDir;
        while (true) {
            i++;
            String dirName = "rinfo-feedchecker-dir-"+i;
            tempDir = new File(System.getProperty("java.io.tmpdir"), dirName);
            if (!tempDir.exists())
                break;
        }
        assert tempDir.mkdir();
        return tempDir;
    }

    protected void removeTempDir() throws Exception {
        FileUtils.forceDelete(tempDir);
    }


    public class LaxStorageSession extends StorageSession {

        private int maxEntries = -1;
        public int getMaxEntries() { return maxEntries; }
        public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }

        private int visitedEntries = 0;

        public LaxStorageSession(StorageCredentials credentials,
                Depot depot,
                Collection<StorageHandler> storageHandlers,
                CollectorLog collectorLog) {
            super(credentials, depot, storageHandlers, collectorLog);
        }

        public boolean storeEntry(Feed sourceFeed, Entry sourceEntry,
                List<SourceContent> contents, List<SourceContent> enclosures) {
            if (maxEntries > -1 && visitedEntries == maxEntries) {
                return false;
            }
            visitedEntries++;
            super.storeEntry(
                    sourceFeed, sourceEntry, contents, enclosures);
            return true; // never break on error
        }
    }

    public class OneFeedCollector extends FeedCollector {

        int archiveCount = 0;

        public OneFeedCollector(StorageSession storageSession) {
            super(storageSession);
        }

        public boolean hasVisitedArchivePage(URL pageUrl) {
            // not visited current; true for anything else
            if (archiveCount >= 1) {
                return true;
            }
            archiveCount++;
            return false;
        }

    }

}
