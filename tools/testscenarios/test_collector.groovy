@Grab('rdfa:rdfa-sesame:0.1.0-SNAPSHOT')
@Grab(group='se.lagrummet.rinfo', module='rinfo-main', version='1.0-SNAPSHOT')
import se.lagrummet.rinfo.collector.atom.FeedArchivePastToPresentReader
import org.apache.abdera.model.*
import org.apache.abdera.i18n.iri.IRI
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.collector.atom.FeedEntryDataIndex
import se.lagrummet.rinfo.collector.atom.fs.FeedEntryDataIndexFSImpl
import se.lagrummet.rinfo.main.storage.EntryRdfValidatorHandler
import se.lagrummet.rinfo.main.storage.SchemaReportException
import se.lagrummet.rinfo.main.storage.StorageCredentials
import se.lagrummet.rinfo.main.storage.StorageSession
import se.lagrummet.rinfo.main.storage.CollectorLog
import se.lagrummet.rinfo.main.storage.CollectorLogSession
import se.lagrummet.rinfo.store.depot.FileDepot


class CollectReader extends FeedArchivePastToPresentReader {

    def rdfValidator

    // from rinfo-main-common.properties
    def checkedBasePath = "/publ/"
    def vocabEntryIds = [
        "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ",
        "http://rinfo.lagrummet.se/ext/vocab"
    ]
    def validationEntryId = "http://rinfo.lagrummet.se/sys/validation"
    def uriSpaceEntryId = "http://rinfo.lagrummet.se/sys/uri"
    def uriSpaceUri = "http://rinfo.lagrummet.se/sys/uri/space#"

    CollectReader(depotDir) {
        rdfValidator = new EntryRdfValidatorHandler(checkedBasePath,
                vocabEntryIds, validationEntryId, uriSpaceEntryId, uriSpaceUri)
        def depot = new FileDepot()
        depot.baseUri = new URI("http://rinfo.lagrummet.se")
        depot.baseDir = new File(depotDir)
        depot.atomizer.feedPath = "/feed"
        depot.initialize()
        // .. or:
        //def components = new Components(config)
        //rdfValidator = components.createEntryRdfValidatorHandler()
        //def depot = components.createDepot()
        def depotSession = depot.openSession()
        def storageSession = new StorageSession(new StorageCredentials(null, true),
                depotSession, [],
                new CollectorLogSession(new CollectorLog(), null) { void start() {}},
                null, null)
        rdfValidator.onStartup(storageSession)
    }

    @Override
    boolean processFeedPage(URL pageUrl, Feed feed) throws Exception {
        //println feedOfFeeds.id
        println "processing <${pageUrl}> with id <${feed.id}>"
        return super.processFeedPage(pageUrl, feed)
    }

    @Override
    void processFeedPageInOrder(URL pageUrl, Feed feed,
            List<Entry> effectiveEntries, Map<IRI, AtomDate> deleteds) {
        println pageUrl
        for (entry in effectiveEntries)
            process(entry)
        if (deleteds.size())
            println "Error: deleted entries should not be!"
    }

    void process(entry) {
        println "<${entry.id}> @ ${entry.updatedElement.string}" as String
        def inStream = getResponseAsInputStream(entry.contentSrc as String)
        try {
            def repo = RDFUtil.createMemoryRepository()
            def uri = entry.id.toURI()
            RDFUtil.loadDataFromStream(repo, inStream,
                    uri as String, entry.contentMimeType as String)
            try {
                rdfValidator.validate(repo, uri)
            } catch (SchemaReportException e) {
                if (e.report.hasErrors) {
                    throw e
                }
            }
        } finally {
            inStream.close();
        }
    }

    @Override
    boolean stopOnEntry(Entry entry) {
        return false
    }

    @Override
    FeedEntryDataIndex getFeedEntryDataIndex() {
        def indexDir = new File("/tmp/test_collector_feed_index")
        if (!indexDir.exists()) indexDir.mkdirs()
        return new FeedEntryDataIndexFSImpl(indexDir)
    }
}


reader = new CollectReader(args[0])
reader.readFeed(new URL(args[1]))
