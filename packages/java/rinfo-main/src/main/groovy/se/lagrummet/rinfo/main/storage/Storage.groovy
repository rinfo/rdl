package se.lagrummet.rinfo.main.storage;

import se.lagrummet.rinfo.store.depot.Depot;
import org.openrdf.repository.Repository;


public class Storage {

    private Depot depot;
    private Repository registryRepo;
    private Collection<StorageHandler> storageHandlers;

    public Storage(Depot depot, Repository registryRepo) {
        this.depot = depot;
        this.registryRepo = registryRepo;
        registryRepo.initialize();
    }

    public Depot getDepot() { return depot; }

    public void startup() {
        StorageSession storageSession = newStorageSession(
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

    public StorageSession newStorageSession(StorageCredentials credentials) {
        // TODO:? depotSession and registrySession?
        def feedCollectorRegistry = new FeedCollectorRegistry(registryRepo);
        return new StorageSession(credentials,
                depot, storageHandlers, feedCollectorRegistry);
    }

    public void shutdown() {
        registryRepo.shutDown();
    }

}
