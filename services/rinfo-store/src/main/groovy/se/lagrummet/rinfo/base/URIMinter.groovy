package se.lagrummet.rinfo.base


import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.sail.SailRepository
import org.openrdf.rio.RDFFormat
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.query.TupleQuery
import org.openrdf.query.QueryLanguage
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter

import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.Templates
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.dom.DOMResult
import javax.xml.xpath.XPathFactory
import javax.xml.xpath.XPathConstants

import se.lagrummet.rinfo.util.rdf.RDFUtil


class URIMinter {

    // TODO: paths in property file? Set each?
    static final String BASE_DATA_FPATH = "datasets/containers.n3"
    static final String COLLECT_URI_DATA_SPARQL = "uri_strategy/collect-uri-data.rq"
    static final String CREATE_URI_XSLT = "uri_strategy/create-uri.xslt"

    String rinfoBaseDir
    Repository baseRepo
    String queryString
    Templates createUriStylesheet

    URIMinter() {
    }

    URIMinter(String rinfoBaseDir) {
        this.setRinfoBaseDir(rinfoBaseDir)
    }

    void setRinfoBaseDir(String rinfoBaseDir) {
        this.rinfoBaseDir = rinfoBaseDir
        def baseDataFpath = pathToCoreFile(BASE_DATA_FPATH)
        def collectUriDataSparql = pathToCoreFile(COLLECT_URI_DATA_SPARQL)
        def createUriXslt = pathToCoreFile(CREATE_URI_XSLT)

        queryString = new File(collectUriDataSparql).text

        createUriStylesheet = TransformerFactory.newInstance().newTemplates(
            new StreamSource(new FileReader(createUriXslt)))

        baseRepo = new SailRepository(new MemoryStore())
        baseRepo.initialize()
        //baseRepo.shutDown()
        RDFUtil.addFile(baseRepo, baseDataFpath, RDFFormat.N3)
    }


    URI computeOfficialUri(Repository repo) {
        def mergedRepo = RDFUtil.createMemoryRepository()
        RDFUtil.addToRepo(mergedRepo, repo)
        RDFUtil.addToRepo(mergedRepo, baseRepo)
        return computeFromMerged(mergedRepo)
    }

    URI computeOfficialUri(String fpath, RDFFormat format) {
        def repo = RDFUtil.createMemoryRepository()
        RDFUtil.addFile(repo, fpath, format)
        RDFUtil.addToRepo(repo, baseRepo)
        return computeFromMerged(repo)
    }

    protected URI computeFromMerged(Repository mergedRepo) {
        def officialUri = null
        try {
            def rqDoc = runQueryToDoc(mergedRepo, queryString)
            officialUri = resultsToUri(rqDoc)
        }
        catch (Exception e) {
            throw e
            //throw new URIComputationException(
            //        "Could not compute canonical URI for: ${fpath}", e)
        }
        return new URI(officialUri)
    }

    private Document runQueryToDoc(repo, queryString) {
        def conn = repo.connection
        def tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString)
        def outStream = new ByteArrayOutputStream()
        tupleQuery.evaluate(new SPARQLResultsXMLWriter(outStream))
        outStream.close()
        def docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.setNamespaceAware(true)
        def builder = docBuilderFactory.newDocumentBuilder()
        return builder.parse(new ByteArrayInputStream(outStream.toByteArray()))
    }

    private String resultsToUri(rqDoc) {
        def domResult = new DOMResult()
        createUriStylesheet.newTransformer().transform(new DOMSource(rqDoc), domResult)
        //printDoc domResult.node
        def xpathExpr = XPathFactory.newInstance().
                newXPath().compile('/entry/id/text()')

        def value = xpathExpr.evaluate(domResult.node)
        if (!value) {
            // TODO: throw new URIComputationException
            // .. eventual error "hint" in results?
        }
        return value
    }

    private String pathToCoreFile(String localFPath) {
        new File(rinfoBaseDir, localFPath) as String
    }


    static printDoc(node) {
        TransformerFactory.newInstance().newTransformer().transform(
            new DOMSource(node), new StreamResult(System.out))
    }


    static main(args) {
        def minter = new URIMinter(args[0])
        args[1..-1].each {
            println minter.computeOfficialUri(it, RDFFormat.RDFXML)
        }
    }

}
