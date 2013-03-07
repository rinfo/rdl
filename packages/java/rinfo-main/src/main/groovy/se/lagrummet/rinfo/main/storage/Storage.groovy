package se.lagrummet.rinfo.main.storage;

import org.openrdf.repository.Repository;

import se.lagrummet.rinfo.store.depot.Depot;
import se.lagrummet.rinfo.collector.atom.FeedEntryDataIndex;


public class Storage {

    private Depot depot;
    private Collection<StorageHandler> storageHandlers;
    private CollectorLog collectorLog;
    private FeedEntryDataIndex feedEntryDataIndex;
    private ErrorLevel stopOnErrorLevel;

    public Storage(Depot depot,
            CollectorLog collectorLog,
            FeedEntryDataIndex feedEntryDataIndex,
            ErrorLevel stopOnErrorLevel) {
        this.depot = depot;
        this.collectorLog = collectorLog;
        this.feedEntryDataIndex = feedEntryDataIndex;
        this.stopOnErrorLevel = stopOnErrorLevel
    }

    public Depot getDepot() { return depot; }
    public Depot getCollectorLog() { return collectorLog; }
    public ErrorLevel getStopOnErrorLevel() { return stopOnErrorLevel; }

    public void startup() {
        StorageSession storageSession = openSession(
                new StorageCredentials(null, true));
        try {
            for (StorageHandler handler : storageHandlers) {
                handler.onStartup(storageSession);
            }
        } finally {
            storageSession.close()
        }
    }

    public Collection<StorageHandler> getStorageHandlers() {
        return storageHandlers;
    }
    public void setStorageHandlers(
            Collection<StorageHandler> storageHandlers) {
        this.storageHandlers = storageHandlers;
    }

    public StorageSession openSession(StorageCredentials credentials) {
        return new StorageSession(credentials, depot.openSession(),
                storageHandlers,
                collectorLog.openSession(),
                feedEntryDataIndex,
                stopOnErrorLevel);
    }

    public void shutdown() {
        collectorLog.shutdown();
    }

}
