package se.lagrummet.rinfo.integration.triplestores;

import org.restlet.Client;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class FormatSupportSparqlTest extends AbstractTestCase {

	/*
	 * Test if SPARQL query supports the following result formats:
	 * 
	 * SPARQL Query Results XML Format	application/sparql-results+xml
	 * SPARQL Query Results JSON Format	application/sparql-results+json
	 * (binary RDF results table format	application/x-binary-rdf-results-table)
	 * (plain text boolean result format	text/boolean)
	 * 
	 */

	public void test() {

		String query = 
			"PREFIX dc:<http://purl.org/dc/elements/1.1/>\n" 
			+ "SELECT ?title WHERE { ?s dc:title ?title }";
		
		Request request = new Request(Method.POST, serviceAppUrl + "/sparql");
        String param = "query=" + query;              
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA);
        
//        ClientInfo clientInfo = request.getClientInfo();              
//        request.setClientInfo(clientInfo)
        
        Client client = new Client(Protocol.HTTP);
        Response response = client.handle(request);       
        
        System.out.println("########### " + response.getEntityAsForm().toString());
        
        assertEquals (Status.SUCCESS_OK , response.getStatus());		
	}
}
