package se.lagrummet.rinfo.base.rdf

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.Templates
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.xpath.XPathExpressionException
import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.query.MalformedQueryException
import org.openrdf.query.QueryEvaluationException
import org.openrdf.query.QueryLanguage
import org.openrdf.query.TupleQuery
import org.openrdf.query.TupleQueryResultHandlerException
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.RepositoryException
import org.openrdf.rio.RDFFormat
import org.openrdf.rio.RDFParseException


class SparqlTree {

    private static final Logger logger = LoggerFactory.getLogger(SparqlTree.class)

    private static final TRANSFORMER_FACTORY = TransformerFactory.newInstance()

    static final SPARQL_XSLT = TRANSFORMER_FACTORY.newTemplates(
            new StreamSource(classPathStream("sparqltree/tree-sparql.xslt")))
    static final TREE_XSLT = TRANSFORMER_FACTORY.newTemplates(
            new StreamSource(classPathStream("sparqltree/tree-transformer.xslt")))

    Repository repo
    File sparqlTreeSrc
    String queryString
    Templates rqToTreeTemplates

    SparqlTree(Repository repo, File sparqlTreeSrc) {
        this.repo = repo
        this.sparqlTreeSrc = sparqlTreeSrc

        def newSource = { new StreamSource(new FileReader(sparqlTreeSrc)) }

        def bytes = new ByteArrayOutputStream()
        SPARQL_XSLT.newTransformer().transform(newSource(), new StreamResult(bytes))
        this.queryString = bytes.toString()

        def inStream = transformToInputStream(TREE_XSLT, newSource())
        this.rqToTreeTemplates = TRANSFORMER_FACTORY.newTemplates(
                new StreamSource(inStream))
    }

    Document queryToTreeDocument() {
        def rqInputStream = queryToInputStream(repo, queryString)
        logger.debug("Transforming to tree..")
        def doc = transformToDocument(rqToTreeTemplates,
                new StreamSource(rqInputStream))
        logger.debug("Tree transform completed.")
        return doc
    }

    void queryAndChainToResult(Result result, Templates ... templates) {
        logger.debug("Transforming results..")

        def saxTransFctry = (SAXTransformerFactory) TRANSFORMER_FACTORY

        def filter = saxTransFctry.newXMLFilter(rqToTreeTemplates)
        for (Templates tplt : templates) {
            def nextFilter = saxTransFctry.newXMLFilter(tplt)
            nextFilter.setParent(filter)
            filter = nextFilter
        }

        def rqInputStream = queryToInputStream(repo, queryString)

        SAXSource transformSource = new SAXSource(
                filter, new InputSource(rqInputStream))
        def chainedTransformer = saxTransFctry.newTransformer()
        chainedTransformer.transform(transformSource, result)

        logger.debug("Transform completed.")
    }

    Document queryToDocument() {
        return SparqlTree.queryToDocument(repo, queryString)
    }

    static Document transformToDocument(Templates xslt, Source source) {
        DOMResult domResult = new DOMResult()
        xslt.newTransformer().transform(source, domResult)
        return (Document) domResult.getNode()
    }

    static InputStream transformToInputStream(Templates xslt, Source source) {
        def outStream = new ByteArrayOutputStream()
        xslt.newTransformer().transform(source, new StreamResult(outStream))
        return new ByteArrayInputStream(outStream.toByteArray())
    }

    protected static InputStream classPathStream(String name) {
        return SparqlTree.getClassLoader().getResourceAsStream(name)
    }

    // TODO: move these to RDFUtil (and reuse in URIMinter as well)
    // .. unless URIMinter should be/use SparqlTree?

    static byte[] queryToByteArray(Repository repo, String queryString) {
        logger.debug("Querying endpoint..")
        def conn = repo.getConnection()
        def tupleQuery = conn.prepareTupleQuery(
                QueryLanguage.SPARQL, queryString)
        def outStream = new ByteArrayOutputStream()
        tupleQuery.evaluate(new SPARQLResultsXMLWriter(outStream))
        try {
            outStream.close()
        } catch (IOException e) {
            throw new RuntimeException("Internal stream error.", e)
        }
        logger.debug("Endpoint query completed.")
        return outStream.toByteArray()
    }

    static InputStream queryToInputStream(Repository repo, String queryString) {
        return new ByteArrayInputStream(queryToByteArray(repo, queryString))
    }

    static Document queryToDocument(Repository repo, String queryString) {
        def rqInputStream = queryToInputStream(repo, queryString)
        def docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.setNamespaceAware(true)
        return docBuilderFactory.newDocumentBuilder().parse(
                rqInputStream)
    }

}
