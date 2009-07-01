package se.lagrummet.rinfo.main.storage;

import se.lagrummet.rinfo.store.depot.DepotEntry;


interface StorageHandler {

    void onStartup(StorageSession storageSession) throws Exception;
    void onCreate(StorageSession storageSession, DepotEntry depotEntry) throws Exception;
    void onUpdate(StorageSession storageSession, DepotEntry depotEntry) throws Exception;
    void onDelete(StorageSession storageSession, DepotEntry depotEntry) throws Exception;

}
