package se.lagrummet.rinfo.store.depot;

public class LockedDepotEntryException extends DepotReadException {

    public LockedDepotEntryException(DepotEntry depotEntry) {
        super(depotEntry+" is locked!");
    }

}
