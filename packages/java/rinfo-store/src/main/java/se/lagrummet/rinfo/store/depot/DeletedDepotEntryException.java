package se.lagrummet.rinfo.store.depot;

public class DeletedDepotEntryException extends DepotReadException {

    public DeletedDepotEntryException(DepotEntry depotEntry) {
        super("Depot entry at "+depotEntry.entryDir+" is deleted!");
    }

}
