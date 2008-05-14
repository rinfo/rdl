package se.lagrummet.rinfo.store.depot

class DuplicateDepotEntryException extends Exception {

    DuplicateDepotEntryException(DepotEntry entry) {
        super("Depot entry at "+entry.entryDir+" already exists!")
    }

}
