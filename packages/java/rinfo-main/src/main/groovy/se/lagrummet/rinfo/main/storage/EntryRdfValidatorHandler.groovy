package se.lagrummet.rinfo.main.storage

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.store.depot.DepotEntry

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.checker.RDFChecker


class EntryRdfValidatorHandler implements StorageHandler {

    URI uriSpaceEntryId
    String checkedBasePath
    String uriSpaceUri

    URIMinter uriMinter
    RDFChecker rdfChecker

    public EntryRdfValidatorHandler(String checkedBasePath,
            String uriSpaceEntryId, String uriSpaceUri) {
        this.uriSpaceEntryId = new URI(uriSpaceEntryId)
        this.checkedBasePath = checkedBasePath
        this.uriSpaceUri = uriSpaceUri
        initRdfChecker()
    }

    void initRdfChecker() {
        this.rdfChecker = new RDFChecker()
        def inStream = getClass().getResourceAsStream("/rdf-checker-config.json")
        try {
            rdfChecker.schemaInfo.loadConfig(inStream)
        } finally {
            inStream.close()
        }
    }

    void onStartup(StorageSession storageSession) throws Exception {
        DepotEntry depotEntry = storageSession.getDepot().getEntry(uriSpaceEntryId)
        if (depotEntry != null) {
            onModified(storageSession, depotEntry, false)
        }
    }

    void onModified(StorageSession storageSession, DepotEntry depotEntry,
            boolean created) throws Exception {
        if (depotEntry.getId().equals(uriSpaceEntryId)) {
            loadContainerData(depotEntry)
        } else {
            validate(depotEntry)
        }
    }

    void onDelete(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
    }

    protected void loadContainerData(DepotEntry depotEntry) {
        uriMinter = new URIMinter(
                EntryRdfReader.readRdf(depotEntry, true), uriSpaceUri)
    }

    protected void validate(DepotEntry depotEntry) {
        Repository repo = EntryRdfReader.readRdf(depotEntry)
        try {
            URI subjectUri = depotEntry.getId()
            hasExpectedUri(subjectUri, repo)
            checkRdf(subjectUri, repo)
        } catch (Exception e) {
            throw e
        } finally {
          repo.shutDown()
        }
    }

    protected void hasExpectedUri(URI subjectUri, Repository repo) {
        if (uriMinter == null) {
            return // TODO: log "no minter available"?
        }
        if (!subjectUri.getPath().startsWith(checkedBasePath)) {
            return
        }
        // TODO: supply entry uri to exclude "false matches"?
        def computedUri = new URI(uriMinter.computeUri(repo))
        if (!subjectUri.equals(computedUri)) {
            throw new IdentifyerMismatchException(subjectUri, computedUri)
        }
    }

    protected void checkRdf(URI subjectUri, Repository repo) {
        def report = rdfChecker.check(repo, subjectUri.toString())
        if (!report.ok) {
            // TODO: new exception carrying individual reports
            throw new Exception("RDFValidationError: ${report.items}")
        }
    }

}
