package se.lagrummet.rinfo.main.storage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.repository.Repository

import org.codehaus.jackson.map.ObjectMapper

import se.lagrummet.rinfo.store.depot.DepotEntry

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.checker.RDFChecker


class EntryRdfValidatorHandler implements StorageHandler {

    private final Logger logger = LoggerFactory.getLogger(EntryRdfValidatorHandler)

    String checkedBasePath
    List<URI> vocabEntryIds
    URI uriSpaceEntryId
    String uriSpaceUri

    URIMinter uriMinter
    RDFChecker rdfChecker

    public EntryRdfValidatorHandler(String checkedBasePath,
            List<String> vocabEntryIds,
            String uriSpaceEntryId, String uriSpaceUri) {
        this.checkedBasePath = checkedBasePath
        this.vocabEntryIds = vocabEntryIds.collect { new URI(it) }
        this.uriSpaceEntryId = new URI(uriSpaceEntryId)
        this.uriSpaceUri = uriSpaceUri
    }

    void onStartup(StorageSession storageSession) throws Exception {
        initUriMinter(storageSession)
        configureRdfChecker(storageSession)
    }

    void initUriMinter(StorageSession storageSession) {
        DepotEntry depotEntry = storageSession.getDepot().getEntry(uriSpaceEntryId)
        if (depotEntry != null) {
            onModified(storageSession, depotEntry, false)
        }
    }

    void configureRdfChecker(StorageSession storageSession, DepotEntry currentEntry=null) {
        def repo = RDFUtil.createMemoryRepository()
        for (URI vocabEntryId : vocabEntryIds) {
            if (currentEntry != null && currentEntry.getId() == vocabEntryId) {
                RDFUtil.addToRepo(repo, EntryRdfReader.readRdf(currentEntry, true))
            } else {
                DepotEntry depotEntry = storageSession.getDepot().getEntry(vocabEntryId)
                if (depotEntry != null) {
                    RDFUtil.addToRepo(repo, EntryRdfReader.readRdf(depotEntry, true))
                }
            }
        }
        rdfChecker = new RDFChecker(repo)
        //def queries = []
        // TODO: get from validationEntryId instead of from hardwired rq files
        //if (currentEntry != null && currentEntry.getId() == validationEntryId) {
        //    queries << EntryReader.getEnclosures().collect { it.text }
        //}
        def queries = [
            "iri_error", "datatype_error", "no_class", "unknown_class",
            "unknown_property", "missing_expected", "expected_datatype",
            "expected_lang", "unexpected_uri_pattern", "from_future",
            "improbable_future", "improbable_past", "spurious_whitespace"
        ].collect {
            getClass().getResourceAsStream("/validation/${it}.rq").getText("utf-8")
        }
        //
        rdfChecker.setTestQueries(queries)
    }

    void onModified(StorageSession storageSession, DepotEntry depotEntry,
            boolean created) throws Exception {
        if (vocabEntryIds.contains(depotEntry.getId())) {
            configureRdfChecker(storageSession, depotEntry)
        } else if (depotEntry.getId().equals(uriSpaceEntryId)) {
            loadUriMinterData(depotEntry)
        } else if (depotEntry.getId().getPath().startsWith(checkedBasePath)) {
            validate(depotEntry)
        }
    }

    void onDelete(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
    }

    protected void loadUriMinterData(DepotEntry depotEntry) {
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
            logger.warn("No URIMinter available.")
            return
        }
        // TODO: supply entry uri to exclude "false matches"?
        def computedUri = new URI(uriMinter.computeUri(repo))
        if (!subjectUri.equals(computedUri)) {
            throw new IdentifyerMismatchException(subjectUri, computedUri)
        }
    }

    protected void checkRdf(URI subjectUri, Repository repo) {
        if (rdfChecker == null) {
            logger.warn("No RDFChecker available.")
            return
        }
        def report = rdfChecker.check(repo, subjectUri.toString())
        if (!report.empty) {
            throw new SchemaReportException(report)
        }
    }

}
