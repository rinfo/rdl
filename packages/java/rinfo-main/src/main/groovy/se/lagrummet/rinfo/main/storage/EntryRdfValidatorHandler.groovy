package se.lagrummet.rinfo.main.storage

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.store.depot.DepotEntry

import se.lagrummet.rinfo.base.URIMinter


class EntryRdfValidatorHandler implements StorageHandler {

    URIMinter uriMinter
    URI containerEntryId
    String checkedBasePath

    public EntryRdfValidatorHandler(URI containerEntryId, String checkedBasePath) {
        this.containerEntryId = containerEntryId
        this.checkedBasePath = checkedBasePath
    }

    void onStartup(StorageSession storageSession) throws Exception {
        DepotEntry depotEntry = storageSession.getDepot().getEntry(containerEntryId)
        if (depotEntry != null) {
            onModified(storageSession, depotEntry, false)
        }
    }

    void onModified(StorageSession storageSession, DepotEntry depotEntry,
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

    protected void loadContainerData(DepotEntry depotEntry) {
        uriMinter = new URIMinter(EntryRdfReader.readRdf(depotEntry))
    }

    protected void validate(DepotEntry depotEntry) {
        Repository repo = EntryRdfReader.readRdf(depotEntry)
        // TODO: run qualify checks in impl(s), based on RDF.TYPE..
        try {
            // TODO: chain of validators (Validator(metaRepo).validate(uri, repo)?)
            hasExpectedUri(depotEntry, repo)
        } catch (Exception e) {
            throw e
        }
        repo.shutDown()
    }

    protected void hasExpectedUri(DepotEntry depotEntry, Repository repo) {
        if (uriMinter == null) {
            return // TODO: log "no minter available"?
        }
        URI subjectUri = depotEntry.getId()
        // TODO: rules for which resources to compute URI:s for:
        // .. if (!rdfType.startsWith(RINFO_PUBL)) return;
        if (!subjectUri.getPath().startsWith(checkedBasePath)) {
            return
        }
        // TODO: supply entry uri to exclude "false matches"?
        def computedUri = uriMinter.computeUri(repo)
        if (!subjectUri.equals(computedUri)) {
            throw new IdentifyerMismatchException(subjectUri, computedUri)
        }
    }

}
