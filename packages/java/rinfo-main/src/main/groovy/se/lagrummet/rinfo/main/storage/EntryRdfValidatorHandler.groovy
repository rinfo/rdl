package se.lagrummet.rinfo.main.storage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.store.depot.DepotEntry

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.checker.RDFChecker


class EntryRdfValidatorHandler implements StorageHandler {

    private final Logger logger = LoggerFactory.getLogger(EntryRdfValidatorHandler)

    String checkedBasePath
    List<URI> vocabEntryIds
    URI validationEntryId
    URI uriSpaceEntryId
    String uriSpaceUri

    URIMinter uriMinter
    RDFChecker rdfChecker

    public EntryRdfValidatorHandler(String checkedBasePath,
            List<String> vocabEntryIds, String validationEntryId,
            String uriSpaceEntryId, String uriSpaceUri) {
        this.checkedBasePath = checkedBasePath
        this.vocabEntryIds = vocabEntryIds.collect { new URI(it) }
        this.validationEntryId = new URI(validationEntryId)
        this.uriSpaceEntryId = new URI(uriSpaceEntryId)
        this.uriSpaceUri = uriSpaceUri
    }

    void onStartup(StorageSession storageSession) throws Exception {
        initUriMinter(storageSession)
        configureRdfChecker(storageSession)
    }

    void onModified(StorageSession storageSession, DepotEntry depotEntry,
            boolean created) throws Exception {
        if (vocabEntryIds.contains(depotEntry.getId())) {
            configureRdfChecker(storageSession, depotEntry)
        } else if (depotEntry.getId().equals(validationEntryId)) {
            configureRdfChecker(storageSession, depotEntry)
        } else if (depotEntry.getId().equals(uriSpaceEntryId)) {
            checkAdminRights(storageSession)
            loadUriMinterData(depotEntry)
        } else if (depotEntry.getId().getPath().startsWith(checkedBasePath)) {
            validate(depotEntry)
        }
    }

    void onDelete(StorageSession storageSession, DepotEntry depotEntry)
            throws Exception {
    }

    protected void checkAdminRights(storageSession) {
        def credentials = storageSession.getCredentials()
        if (!credentials.isAdmin()) {
            throw new NotAllowedException(
                    "Not allowed to configure validation: not an admin source!", credentials)
        }
    }

    void configureRdfChecker(StorageSession storageSession, DepotEntry currentEntry=null) {
        checkAdminRights(storageSession)
        def repo = RDFUtil.createMemoryRepository()
        for (URI vocabEntryId : vocabEntryIds) {
            DepotEntry vocabEntry =
                (currentEntry != null && currentEntry.getId() == vocabEntryId)?
                    currentEntry : storageSession.getDepot().getEntry(vocabEntryId)
            if (vocabEntry != null) {
                RDFUtil.addToRepo(repo, EntryRdfReader.readRdf(vocabEntry, true))
            }
        }
        def queries = (rdfChecker != null)? rdfChecker.getTestQueries() : []
        DepotEntry validationEntry =
            (currentEntry != null && currentEntry.getId() == validationEntryId)?
                    currentEntry : storageSession.getDepot().getEntry(validationEntryId)
        if (validationEntry != null) {
            queries = []
            for (encl in validationEntry.findEnclosures()) {
                if (encl.depotUriPath.endsWith(".rq") ||
                        encl.mediaType == "application/sparql-query") {
                    queries << encl.file.getText("utf-8")
                }
            }
        }
        rdfChecker = new RDFChecker(repo)
        if (queries) {
            rdfChecker.setTestQueries(queries)
        }
    }

    void initUriMinter(StorageSession storageSession) {
        DepotEntry depotEntry = storageSession.getDepot().getEntry(uriSpaceEntryId)
        if (depotEntry != null) {
            loadUriMinterData(depotEntry)
        }
    }

    void loadUriMinterData(DepotEntry depotEntry) {
        uriMinter = new URIMinter(
                EntryRdfReader.readRdf(depotEntry, true), uriSpaceUri)
    }

    void validate(DepotEntry depotEntry) {
        def repo = EntryRdfReader.readRdf(depotEntry)
        try {
            validate(repo, depotEntry.getId())
        } finally {
          repo.shutDown()
        }
    }

    void validate(Repository repo, URI subjectUri) {
        hasExpectedUri(subjectUri, repo)
        checkRdf(subjectUri, repo)
    }

    void hasExpectedUri(URI subjectUri, Repository repo) {
        if (uriMinter == null) {
            logger.warn("No URIMinter available.")
            return
        }
        logger.warn("subjectUri='"+subjectUri+"'")
        def uriResultMap = uriMinter.computeUris(repo)
        logger.warn("uriResultMap.size="+(uriResultMap!=null?""+uriResultMap.size():"-"))
        for (String key : uriResultMap.keySet())
            logger.warn("uriResultMap["+key+"]='"+uriResultMap.get(key)+"'")
        def uriResults = uriResultMap[subjectUri.toString()]
        logger.warn("uriResults.size="+(uriResults!=null?""+uriResults.size():"-"))
        def uriStr = (uriResults && uriResults.size() > 0)? uriResults[0].uri : null
        if (uriStr == null) {
            throw new UnknownSubjectException(subjectUri)
            /*for (results in uriResultMap.values()) {
                for (result in results) {
                    uriStr = result.uri
                    break
                }
            } */
        }
        def computedUri = uriStr != null? new URI(uriStr) : null
        if (!computedUri)
            throw new UnableToComputeValidationURI(subjectUri)
        if (!subjectUri.equals(computedUri)) {
            throw new IdentifyerMismatchException(subjectUri, computedUri)
        }
    }

    void checkRdf(URI subjectUri, Repository repo) {
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
