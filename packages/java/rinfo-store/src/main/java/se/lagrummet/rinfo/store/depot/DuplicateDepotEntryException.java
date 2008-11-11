package se.lagrummet.rinfo.store.depot;

public class DuplicateDepotEntryException extends DepotWriteException {

    public DuplicateDepotEntryException(DepotEntry entry) {
        super("Depot entry at "+entry.entryDir+" already exists!");
    }

}
