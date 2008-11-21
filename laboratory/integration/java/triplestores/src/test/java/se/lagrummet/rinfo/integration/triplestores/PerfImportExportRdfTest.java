package se.lagrummet.rinfo.integration.triplestores;

import java.io.File;

import org.restlet.Client;
import org.restlet.Component;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import se.lagrummet.rinfo.integration.triplestores.util.FileApplication;

/**
 * Test performance of import and export of RDF to/from repository.
 * 
 * See properties file for setting the source file to use in load test.
 * 
 * @author msher
 */
public class PerfImportExportRdfTest extends AbstractTestCase {
	
	private static Component component;
    private String rdfAppUrl;
	
    /*
     * Set up a simple application that serves (RDF) files.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();        
        
        String rdfAppDir = new File("src/test/resources/rdf").toURI().toString();
        int rdfAppPort = 8282;
        rdfAppUrl = "http://localhost:"  + rdfAppPort + "/" + rdfFile;
        
        component = new Component();
        component.getServers().add(Protocol.HTTP, rdfAppPort);
        component.getClients().add(Protocol.FILE);
        component.getDefaultHost().attach(new FileApplication(rdfAppDir));
        component.start();
    }
    
    @Override
    protected void tearDown() throws Exception {
    	component.stop();
    	super.tearDown();   
    }	

    public void testConnectRdfApp() {
		Request request = new Request(Method.GET, rdfAppUrl);
        Client client = new Client(Protocol.HTTP);     
        Response response = client.handle(request);
        assertEquals (Status.SUCCESS_OK , response.getStatus());    	
    }

    /*
     * TODO: measure time to retrieve all statements - (if of interest)
     */
//	public void testExportRdf() {		
//		Request request = new Request(Method.GET, serviceAppUrl + "/rdf");
//        Client client = new Client(Protocol.HTTP);     
//        Response response = client.handle(request);
//        assertEquals (Status.SUCCESS_OK , response.getStatus());
//	}

    /**
     * Verify that ServiceApplication accepts POST to '/rdf' with URL to a
     * RDF/XML file to import to repository.
     * 
     * Measure time from request to response.
     */
	public void testImportRdf() throws Exception {
		Request request = new Request(Method.POST, serviceAppUrl + "/rdf");
        String param = "url=" + rdfAppUrl;
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA);     
        Client client = new Client(Protocol.HTTP);
        
        // perform and measure elapsed time
        long time = System.currentTimeMillis();
        Response response = client.handle(request);       
        time = System.currentTimeMillis() - time;        
        reportWriter.addImportTestResult(time);
        
        assertEquals (Status.SUCCESS_OK , response.getStatus());            
	}

}
