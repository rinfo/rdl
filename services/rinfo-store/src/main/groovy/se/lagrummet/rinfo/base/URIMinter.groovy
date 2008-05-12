package se.lagrummet.rinfo.base


/*
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
import org.apache.xpath.XPathAPI
*/


class URIMinter {
/*

    // TODO: put into property file
    static final String BASE_DATA_FPATH = "rdf_data/resources/containers.n3"
    static final String COLLECT_URI_DATA_SPARQL = "uri_strategy/collect-uri-data.rq"
    static final String CREATE_URI_XSLT = "uri_strategy/create-uri.xslt"

    String rinfoBaseDir
    Repository baseRepo
    String queryString
    Templates createUriStylesheet

    URIMinter(String rinfoBaseDir) {
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
        addFile(baseRepo, baseDataFpath, RDFFormat.N3)
    }


    String computeOfficialUri(fpath, format) {
        def mergedRepo = mergeWithBaseRepo(fpath, format)
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
        return officialUri
    }

    private Repository mergeWithBaseRepo(fpath, format) {
        def docRepo = new SailRepository(new MemoryStore())
        docRepo.initialize()
        addFile(docRepo, fpath, format)
        addToRepo(docRepo, baseRepo)
        return docRepo
    }

    private Document runQueryToDoc(repo, queryString) {
        def conn = repo.connection
        def tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString)
        def sourceIn = new PipedInputStream()
        def sourceOut = new PipedOutputStream(sourceIn)
        tupleQuery.evaluate(new SPARQLResultsXMLWriter(sourceOut))
        sourceOut.close()
        def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return builder.parse(sourceIn)
    }

    private String resultsToUri(rqDoc) {
        def domResult = new DOMResult()
        createUriStylesheet.newTransformer().transform(new DOMSource(rqDoc), domResult)
        //printDoc domResult.node
        def nodeList = XPathAPI.selectNodeList(domResult.node, '/entry/id/text()')
        if (nodeList.length == 0) {
            // TODO: throw new URIComputationException
        }
        return nodeList.item(0).nodeValue
    }

    private pathToCoreFile(localFPath) {
        new File(rinfoBaseDir, localFPath) as String
    }


    static void addFile(repo, fpath, RDFFormat format) {
        def file = new File(fpath)
        String baseURI = file.toURI()
        def conn = repo.connection
        conn.add(file, baseURI, format)
        conn.commit()
    }

    static addToRepo(targetRepo, repoToAdd) {
        def targetConn = targetRepo.connection
        def connToAdd = repoToAdd.connection
        targetConn.add(connToAdd.getStatements(null, null, null, true))
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

*/
}
