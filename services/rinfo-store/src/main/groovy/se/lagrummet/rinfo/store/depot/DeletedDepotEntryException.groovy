package se.lagrummet.rinfo.store.depot

class DeletedDepotEntryException extends Exception {

    DeletedDepotEntryException(DepotEntry depotEntry) {
        super("Depot entry at "+depotEntry.entryDir+" is deleted!")
    }

}
