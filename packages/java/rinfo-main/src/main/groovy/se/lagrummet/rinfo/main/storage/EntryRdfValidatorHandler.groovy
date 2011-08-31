package se.lagrummet.rinfo.main.storage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.repository.Repository

import org.codehaus.jackson.map.ObjectMapper

import se.lagrummet.rinfo.store.depot.DepotEntry

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.checker.RDFChecker
import se.lagrummet.rinfo.base.rdf.checker.SchemaInfoTool


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
        def config = null
        def mapper = new ObjectMapper()
        def inStream = getClass().getResourceAsStream("/rdf-checker-config.json")
        try {
            config = mapper.readValue(inStream, Map)
        } finally {
            inStream.close()
        }
        def tool = new SchemaInfoTool()
        def repo = RDFUtil.createMemoryRepository()
        for (URI entryId : vocabEntryIds) {
            if (currentEntry != null && currentEntry.getId() == entryId) {
                RDFUtil.addToRepo(repo, EntryRdfReader.readRdf(currentEntry, true))
            } else {
                DepotEntry depotEntry = storageSession.getDepot().getEntry(entryId)
                if (depotEntry != null) {
                    RDFUtil.addToRepo(repo, EntryRdfReader.readRdf(depotEntry, true))
                }
            }
        }
        def data = tool.getSchemaData(repo)
        tool.extendSchemaData(data, [config])
        this.rdfChecker = new RDFChecker()
        rdfChecker.schemaInfo.configure(data)
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
        if (!report.ok) {
            // TODO: new exception carrying individual reports
            throw new Exception("RDFValidationError: ${report.items.join('\n')}")
        }
    }

}
