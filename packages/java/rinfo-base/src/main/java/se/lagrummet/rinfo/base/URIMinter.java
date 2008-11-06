package se.lagrummet.rinfo.base;

import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;

import org.apache.commons.io.FileUtils;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import se.lagrummet.rinfo.base.rdf.RDFUtil;


public class URIMinter {

    // TODO: paths in property file? Set each?
    public static final String BASE_DATA_FPATH = "datasets/containers.n3";
    public static final String COLLECT_URI_DATA_SPARQL = "uri_algorithm/collect-uri-data.rq";
    public static final String CREATE_URI_XSLT = "uri_algorithm/create-uri.xslt";

    private String rinfoBaseDir;

    private Repository baseRepo;
    private String queryString;
    private Templates createUriStylesheet;

    public URIMinter() {
    }

    public URIMinter(String rinfoBaseDir)
        throws IOException, FileNotFoundException, RepositoryException
    {
        this.setRinfoBaseDir(rinfoBaseDir);
    }

    public String getRinfoBaseDir() {
        return rinfoBaseDir;
    }

    public void setRinfoBaseDir(String rinfoBaseDir)
        throws IOException, FileNotFoundException, RepositoryException
    {
        this.rinfoBaseDir = rinfoBaseDir;
        String baseDataFpath = pathToCoreFile(BASE_DATA_FPATH);
        String collectUriDataSparql = pathToCoreFile(COLLECT_URI_DATA_SPARQL);
        String createUriXslt = pathToCoreFile(CREATE_URI_XSLT);

        queryString = FileUtils.readFileToString(
                new File(collectUriDataSparql));

        try {
            createUriStylesheet = TransformerFactory.newInstance().newTemplates(
                new StreamSource(new FileReader(createUriXslt)));

            baseRepo = RDFUtil.createMemoryRepository();
            //baseRepo.shutDown()
            RDFUtil.addFile(baseRepo, baseDataFpath, RDFFormat.N3);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(
                    "Caught TransformerConfigurationException.", e);
        } catch (RDFParseException e) {
            throw new RuntimeException("Malformed RDF base data.", e);
        }
    }


    public URI computeOfficialUri(Repository repo)
        throws RepositoryException, URIComputationException
    {
        Repository mergedRepo = RDFUtil.createMemoryRepository();
        RDFUtil.addToRepo(mergedRepo, repo);
        RDFUtil.addToRepo(mergedRepo, baseRepo);
        return computeFromMerged(mergedRepo, repo.toString());
    }

    public URI computeOfficialUri(String fpath, RDFFormat format)
        throws IOException,
               RepositoryException, RDFParseException,
               URIComputationException
    {
        Repository repo = RDFUtil.createMemoryRepository();
        RDFUtil.addFile(repo, fpath, format);
        RDFUtil.addToRepo(repo, baseRepo);
        return computeFromMerged(repo, fpath);
    }

    protected URI computeFromMerged(Repository mergedRepo, String sourceHint)
        throws RepositoryException, URIComputationException
    {
        String officialUri = null;
        try {
            Document rqDoc = runQueryToDoc(mergedRepo, queryString);
            officialUri = resultsToUri(rqDoc);
            return new URI(officialUri);
        }
        catch (Exception e) {
            throw new URIComputationException(
                    "Could not compute canonical URI for: "+sourceHint, e);
        }
    }

    private Document runQueryToDoc(Repository repo, String queryString)
        throws IOException,
               RepositoryException,
               MalformedQueryException, QueryEvaluationException,
               TupleQueryResultHandlerException,
               ParserConfigurationException, SAXException,
               XPathExpressionException
    {
        RepositoryConnection conn = repo.getConnection();
        TupleQuery tupleQuery = conn.prepareTupleQuery(
                QueryLanguage.SPARQL, queryString);

        // TODO: use to wire in the known subject URI
        //def about = "http://example.org/data/sfs/1999:175"
        /*
        if (about != null) {
            tupleQuery.setBinding("about", repo.valueFactory.createURI(about))
        }
        */

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        tupleQuery.evaluate(new SPARQLResultsXMLWriter(outStream));
        try {
            outStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Internal stream error.", e);
        }
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(outStream.toByteArray()));
    }

    private String resultsToUri(Document rqDoc)
        throws TransformerConfigurationException, TransformerException, XPathExpressionException
    {
        DOMResult domResult = new DOMResult();
        createUriStylesheet.newTransformer().transform(new DOMSource(rqDoc), domResult);
        //printDoc domResult.node
        XPathExpression xpathExpr = XPathFactory.newInstance().
                newXPath().compile("/entry/id/text()");

        String value = (String) xpathExpr.evaluate(domResult.getNode());
        if (value == null || value.equals("")) {
            // TODO: throw new URIComputationException
            // .. eventual error "hint" in results?
        }
        return value;
    }

    private String pathToCoreFile(String localFPath) {
        return new File(rinfoBaseDir, localFPath).toString();
    }


    /* TODO: debug; remove?
    static printDoc(node) {
        TransformerFactory.newInstance().newTransformer().transform(
            new DOMSource(node), new StreamResult(System.out));
    }
    */

    public static void main(String[] args)
        throws IOException, RepositoryException, RDFParseException,
               URIComputationException {
        String rinfoBaseDir = args[0];
        URIMinter minter = new URIMinter(rinfoBaseDir);
        for (int i = 1; i < args.length; i++) {
            System.out.println(minter.computeOfficialUri(args[i], RDFFormat.RDFXML));
        }
    }

}
