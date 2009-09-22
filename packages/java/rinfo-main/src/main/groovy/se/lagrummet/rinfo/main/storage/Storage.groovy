package se.lagrummet.rinfo.main.storage;

import se.lagrummet.rinfo.store.depot.Depot;
import org.openrdf.repository.Repository;


public class Storage {

    private Depot depot;
    private CollectorLog collectorLog;
    private Collection<StorageHandler> storageHandlers;

    public Storage(Depot depot, CollectorLog collectorLog) {
        this.depot = depot;
        this.collectorLog = collectorLog;
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
        return new StorageSession(credentials,
                depot, storageHandlers, collectorLog);
    }

    public void shutdown() {
        collectorLog.shutdown();
    }

}
