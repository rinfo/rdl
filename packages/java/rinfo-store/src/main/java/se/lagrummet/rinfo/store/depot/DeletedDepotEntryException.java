package se.lagrummet.rinfo.store.depot;

public class DeletedDepotEntryException extends DepotReadException {

    public DeletedDepotEntryException(DepotEntry depotEntry) {
        super(depotEntry+" is deleted!");
    }

}
