package se.lagrummet.rinfo.store.depot;

public class DuplicateDepotContentException extends RuntimeException {

    public DuplicateDepotContentException(DepotEntry depotEntry,
            String mediaType, String lang) {
        super("Depot content for media type " + mediaType +
                " and language " + lang + " already exists in depot entry" +
                depotEntry.getId());
    }

}
