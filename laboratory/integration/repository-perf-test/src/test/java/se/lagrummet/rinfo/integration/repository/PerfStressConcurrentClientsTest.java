package se.lagrummet.rinfo.integration.repository;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.restlet.Client;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

public class PerfStressConcurrentClientsTest extends AbstractTestCase {

	/*
	 * 1 request per client
	 * increasing amount of clients.
	 * 
	 * 1000 requests per client
	 * increasing amount of clients.
	 * 
	 */
	
	private static final String TITLE_QUERY = 
		"PREFIX dc:<http://purl.org/dc/elements/1.1/>\n" 
		+ "SELECT ?title WHERE { ?s dc:title ?title }";

	private static List<Preference<MediaType>> acceptedMediaTypeSparqlResultsXml;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

//        acceptedMediaTypeSparqlResultsXml = new ArrayList<Preference<MediaType>>();
//        acceptedMediaTypeSparqlResultsXml.add(new Preference<MediaType>(
//        		Constants.MEDIA_TYPE_SPARQL_RESULTS_XML));
    }

	
	public void test() {
	}

//	private Request prepareGetRequest(List<Preference<MediaType>> mediaTypes, 
//			String query) throws IOException {
//		String q = URLEncoder.encode(query, "UTF-8");		
//		Request request = 
//			new Request(Method.GET, serviceAppUrl + "/sparql?query=" + q);        		
//        request.getClientInfo().setAcceptedMediaTypes(mediaTypes);
//        return request; 
//	}
//
//	public void testGetTupleXml() throws IOException {
//		Request request = prepareGetRequest(
//				acceptedMediaTypeSparqlResultsXml, TITLE_QUERY);		
//        Client client = new Client(Protocol.HTTP);
//        Response response = client.handle(request);       
//        assertEquals(Status.SUCCESS_OK , response.getStatus());		
//
//        Representation rep = response.getEntity();        
//        assertEquals(
//        		rep.getMediaType().getName(), 
//        		Constants.MIME_TYPE_SPARQL_RESULTS_XML);
//        
//        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");
//	}

}
