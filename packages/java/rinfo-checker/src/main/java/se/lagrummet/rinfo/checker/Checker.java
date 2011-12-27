package se.lagrummet.rinfo.checker;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.io.FileUtils;

import org.apache.http.client.HttpClient;

import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;
import org.apache.abdera.i18n.iri.IRI;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import se.lagrummet.rinfo.store.depot.Depot;
import se.lagrummet.rinfo.store.depot.DepotSession;
import se.lagrummet.rinfo.store.depot.FileDepot;
import se.lagrummet.rinfo.store.depot.SourceContent;

import se.lagrummet.rinfo.collector.atom.CompleteFeedEntryIdIndex;
import se.lagrummet.rinfo.main.storage.ErrorLevel;
import se.lagrummet.rinfo.main.storage.FeedCollector;
import se.lagrummet.rinfo.main.storage.FeedCollectorSession;
import se.lagrummet.rinfo.main.storage.StorageSession;
import se.lagrummet.rinfo.main.storage.StorageCredentials;
import se.lagrummet.rinfo.main.storage.StorageHandler;
import se.lagrummet.rinfo.main.storage.CollectorLog;
import se.lagrummet.rinfo.main.storage.CollectorLogSession;
import se.lagrummet.rinfo.main.storage.CollectorSource;


public class Checker {

    Repository logRepo = new SailRepository(new MemoryStore());
    String reportBaseUri;
    String entryDatasetUri;

    Depot depot;
    File tempDir;

    private List<StorageHandler> handlers;
    public List<StorageHandler> getHandlers() { return handlers; }
    public void setHandlers(List<StorageHandler> handlers) { this.handlers = handlers; }

    private int maxEntries = -1;
    public int getMaxEntries() { return maxEntries; }
    public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }

    public Checker(String reportBaseUri,
            String entryDatasetUri) throws Exception {
        this.reportBaseUri = reportBaseUri;
        this.entryDatasetUri = entryDatasetUri;
        tempDir = createTempDir();
        depot = new FileDepot(new URI("http://rinfo.lagrummet.se"), tempDir);
        depot.getAtomizer().setFeedPath("/feed");
        ((FileDepot) depot).initialize();
        logRepo.initialize();
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
        CollectorLog coLog = new CollectorLog(logRepo, reportBaseUri, entryDatasetUri);
        URI sourceId = new URI("tag:"+ feedUrl.getHost() +",1900:unknown:feed");
        StorageCredentials credentials = new StorageCredentials(
                new CollectorSource(sourceId, feedUrl),
                adminSource);
        LaxStorageSession storageSession = new LaxStorageSession(
                credentials,
                depot.openSession(), handlers, coLog.openSession());
        storageSession.setMaxEntries(maxEntries);
        FeedCollectorSession collectSession = new OneFeedCollectorSession(
                FeedCollector.createDefaultClient(), storageSession);
        collectSession.readFeed(feedUrl);
        collectSession.shutdown();
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


    public static class OneFeedCollectorSession extends FeedCollectorSession {

        int archiveCount = 0;

        public OneFeedCollectorSession(HttpClient httpClient,
                StorageSession storageSession) {
            super(httpClient, storageSession);
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


    public static class LaxStorageSession extends StorageSession {

        private int maxEntries = -1;
        public int getMaxEntries() { return maxEntries; }
        public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }

        private int visitedEntries = 0;

        public LaxStorageSession(StorageCredentials credentials,
                DepotSession depotSession,
                Collection<StorageHandler> storageHandlers,
                CollectorLogSession collectorLogSession) {
            super(credentials, depotSession, storageHandlers,
                    collectorLogSession, new NoopFeedEntryIdIndex(),
                    ErrorLevel.NONE);
        }

        public boolean storeEntry(Feed sourceFeed, Entry sourceEntry,
                List<SourceContent> contents, List<SourceContent> enclosures) {
            if (maxEntries > -1 && visitedEntries == maxEntries) {
                return false;
            }
            visitedEntries++;
            return super.storeEntry(
                    sourceFeed, sourceEntry, contents, enclosures);
        }
    }


    public static class NoopFeedEntryIdIndex implements CompleteFeedEntryIdIndex {
        public Set<IRI> getEntryIdsForCompleteFeedId(IRI feedId) {
            return null;
        }
        public void storeEntryIdsForCompleteFeedId(IRI feedId, Set<IRI> entryIds) {
            ;
        }
    }


}
