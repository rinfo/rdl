package se.lagrummet.rinfo.main.storage;

import se.lagrummet.rinfo.store.depot.DepotEntry;


/**
 * A base handler which forwards onCreate and onUpdate to onEntry.
 */
abstract class AbstractStorageHandler implements StorageHandler {

    void onCreate(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
        onEntry(storageSession, depotEntry, true);
    }

    void onUpdate(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
        onEntry(storageSession, depotEntry, false);
    }

    abstract void onEntry(StorageSession storageSession, DepotEntry depotEntry,
            boolean created) throws Exception;

}
