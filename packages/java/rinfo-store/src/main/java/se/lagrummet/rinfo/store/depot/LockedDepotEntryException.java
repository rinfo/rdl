package se.lagrummet.rinfo.store.depot;

public class LockedDepotEntryException extends Exception {

    public LockedDepotEntryException(DepotEntry depotEntry) {
        super("Depot entry at "+depotEntry.entryDir+" is locked!");
    }

}
