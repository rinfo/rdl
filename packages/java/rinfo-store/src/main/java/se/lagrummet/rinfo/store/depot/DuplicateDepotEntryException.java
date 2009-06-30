package se.lagrummet.rinfo.store.depot;

public class DuplicateDepotEntryException extends DepotWriteException {

    public DuplicateDepotEntryException(DepotEntry depotEntry) {
        super(depotEntry+" already exists!");
    }

}
