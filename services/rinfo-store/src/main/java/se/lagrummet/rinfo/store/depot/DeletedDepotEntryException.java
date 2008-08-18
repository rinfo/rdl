package se.lagrummet.rinfo.store.depot;

public class DeletedDepotEntryException extends Exception {

    public DeletedDepotEntryException(DepotEntry depotEntry) {
        super("Depot entry at "+depotEntry.entryDir+" is deleted!");
    }

}
