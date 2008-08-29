package se.lagrummet.rinfo.store.depot;

public class DuplicateDepotEntryException extends Exception {

    public DuplicateDepotEntryException(DepotEntry entry) {
        super("Depot entry at "+entry.entryDir+" already exists!");
    }

}
