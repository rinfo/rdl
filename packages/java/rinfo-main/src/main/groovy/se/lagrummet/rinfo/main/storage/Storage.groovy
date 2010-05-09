package se.lagrummet.rinfo.main.storage;

import org.openrdf.repository.Repository;

import se.lagrummet.rinfo.store.depot.Depot;
import se.lagrummet.rinfo.collector.atom.CompleteFeedEntryIdIndex;


public class Storage {

    private Depot depot;
    private Collection<StorageHandler> storageHandlers;
    private CollectorLog collectorLog;
    private CompleteFeedEntryIdIndex completeFeedEntryIdIndex;

    public Storage(Depot depot,
            CollectorLog collectorLog,
            CompleteFeedEntryIdIndex completeFeedEntryIdIndex) {
        this.depot = depot;
        this.collectorLog = collectorLog;
        this.completeFeedEntryIdIndex = completeFeedEntryIdIndex;
    }

    public Depot getDepot() { return depot; }
    public Depot getCollectorLog() { return collectorLog; }

    public void startup() {
        StorageSession storageSession = openSession(
                new StorageCredentials(true));
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
        return new StorageSession(credentials, depot, storageHandlers,
                collectorLog.openSession(), completeFeedEntryIdIndex);
    }

    public void shutdown() {
        collectorLog.shutdown();
    }

}
