package se.lagrummet.rinfo.main.storage

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.store.depot.DepotEntry

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.URIComputationException


class EntryRdfValidatorHandler extends AbstractStorageHandler {

    URIMinter uriMinter
    URI containerEntryId

    public EntryRdfValidatorHandler(URIMinter uriMinter, URI containerEntryId) {
        this.uriMinter = uriMinter
        this.containerEntryId = containerEntryId
    }

    void onStartup(StorageSession storageSession) throws Exception {
        DepotEntry depotEntry = storageSession.getDepot().getEntry(containerEntryId)
        if (depotEntry != null) {
            onEntry(storageSession, depotEntry, false)
        }
    }

    void onEntry(StorageSession storageSession, DepotEntry depotEntry,
            boolean created) throws Exception {
        if (depotEntry.getId().equals(containerEntryId)) {
            loadContainerData(depotEntry)
        } else {
            validate(depotEntry)
        }
    }

    void onDelete(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
    }

    protected void loadContainerData(depotEntry) {
        uriMinter.setRepo(EntryRdfReader.readRdf(depotEntry))
    }

    protected void validate(depotEntry) {
        Repository repo = EntryRdfReader.readRdf(depotEntry)
        // TODO: qualify checks in impl(s), based on RDF.TYPE..
        try {
            // TODO: chain of validators (Validator(metaRepo).validate(uri, repo)?)
            hasExpectedUri(depotEntry.id, repo)
        } catch (Exception e) {
            // TODO: rules for which resources to compute URI:s for
        }
        repo.shutDown()
    }

    protected void hasExpectedUri(URI subject, repo) {
        // TODO: rules for which resources to compute URI:s for:
        // .. if (!rdfType.startsWith(RINFO_PUBL)) return;
        // .. or just: if (!subject.startsWith(forBaseUri)) return;
        try {
            // TODO: supply subject?
            def newUri = uriMinter.computeOfficialUri(repo)
            if (!depotEntry.id.equals(newUri)) {
                throw new IllegalEntryIdException(depotEntry)
            }
            hasExpectedUri(depotEntry.id, repo)
        } catch (URIComputationException e) {
            // TODO: don't catch when rules for which to compute for is in place.
        }
    }

}


class IllegalEntryIdException extends Exception {
    IllegalEntryIdException(DepotEntry depotEntry) {
        super(depotEntry.toString())
    }
}
