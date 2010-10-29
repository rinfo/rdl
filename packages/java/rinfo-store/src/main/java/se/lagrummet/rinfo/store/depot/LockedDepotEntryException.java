package se.lagrummet.rinfo.store.depot;

public class LockedDepotEntryException extends DepotReadException {

    private DepotEntry lockedEntry;

    public LockedDepotEntryException(DepotEntry depotEntry) {
        super(depotEntry+" is locked!");
        lockedEntry = depotEntry;
    }

    public DepotEntry getLockedEntry() {
        return lockedEntry;
    }

}
