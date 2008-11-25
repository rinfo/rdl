package se.lagrummet.rinfo.integration.triplestores;

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

import se.lagrummet.rinfo.integration.triplestores.domain.data.Constants;

/**
 * Verify that the SPARQL endpoint can handle SPARQL queries sent by GET and 
 * POST. Furthermore that when the following mime types are given as accepted
 * types, the endpoint delivers content in the given type:
 * 
 * <ul>
 * 	<li>application/sparql-results+xml</li>
 * 	<li>application/sparql-results+json</li>
 * 	<li>text/boolean</li>
 * </ul>
 * 
 * N.B. Validation of returned media type is made on name level only, no other
 * parameters are compared. 
 * 
 * @author msher
 */
public class FormatSupportSparqlTest extends AbstractTestCase {

	private static final String TITLE_QUERY = 
		"PREFIX dc:<http://purl.org/dc/elements/1.1/>\n" 
		+ "SELECT ?title WHERE { ?s dc:title ?title }";

	private static final String ASK_QUERY = "ask {?s ?p ?o}";		
	
	private static List<Preference<MediaType>> acceptedMediaTypeSparqlResultsXml;
	private static List<Preference<MediaType>> acceptedMediaTypeSparqlResultsJson;
	private static List<Preference<MediaType>> acceptedMediaTypeTextBoolean;
	private static List<Preference<MediaType>> acceptedMediaTypeTextPlain;
	
	
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        acceptedMediaTypeSparqlResultsXml = new ArrayList<Preference<MediaType>>();
        acceptedMediaTypeSparqlResultsXml.add(new Preference<MediaType>(
        		Constants.MEDIA_TYPE_SPARQL_RESULTS_XML));

        acceptedMediaTypeSparqlResultsJson = new ArrayList<Preference<MediaType>>();
        acceptedMediaTypeSparqlResultsJson.add(new Preference<MediaType>(
        		Constants.MEDIA_TYPE_SPARQL_RESULTS_JSON));

        acceptedMediaTypeTextBoolean = new ArrayList<Preference<MediaType>>();
        acceptedMediaTypeTextBoolean.add(new Preference<MediaType>(
        		Constants.MEDIA_TYPE_TEXT_BOOLEAN));

        acceptedMediaTypeTextPlain = new ArrayList<Preference<MediaType>>();
        acceptedMediaTypeTextPlain.add(new Preference<MediaType>(MediaType.TEXT_PLAIN));
    }
    
    
	public void testGetBooleanXml() throws IOException {		
		Request request = prepareGetRequest(
				acceptedMediaTypeSparqlResultsXml, ASK_QUERY);		
        Client client = new Client(Protocol.HTTP);        
        Response response = client.handle(request);		
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		Constants.MIME_TYPE_SPARQL_RESULTS_XML);

        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");
	}

	public void testGetTupleXml() throws IOException {
		Request request = prepareGetRequest(
				acceptedMediaTypeSparqlResultsXml, TITLE_QUERY);		
        Client client = new Client(Protocol.HTTP);
        Response response = client.handle(request);       
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		Constants.MIME_TYPE_SPARQL_RESULTS_XML);
        
        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");
	}
	
	public void testGetJson() throws IOException {		
		Request request = prepareGetRequest(
				acceptedMediaTypeSparqlResultsJson, TITLE_QUERY);		
        Client client = new Client(Protocol.HTTP);
        Response response = client.handle(request);       
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		Constants.MIME_TYPE_SPARQL_RESULTS_JSON);
        
        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");
	}
	
	public void testGetTextBoolean() throws IOException {		
		Request request = prepareGetRequest(
				acceptedMediaTypeTextBoolean, ASK_QUERY);		
        Client client = new Client(Protocol.HTTP);
        Response response = client.handle(request);       
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		Constants.MIME_TYPE_TEXT_BOOLEAN);
        
        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");
	}

	public void testGetBooleanTextPlain() throws IOException {		
		Request request = prepareGetRequest(
				acceptedMediaTypeTextPlain, ASK_QUERY);				
        Client client = new Client(Protocol.HTTP);        
        Response response = client.handle(request);
		
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(rep.getMediaType().getName(), MediaType.TEXT_PLAIN.getName());

        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");
	}

	public void testGetTupleTextPlain() throws IOException {		
		Request request = prepareGetRequest(
				acceptedMediaTypeTextPlain, TITLE_QUERY);				
        Client client = new Client(Protocol.HTTP);
        Response response = client.handle(request);       
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(rep.getMediaType().getName(), MediaType.TEXT_PLAIN.getName());
        
        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");
	}
	
	public void testPostBooleanXml() throws IOException {				
		Request request = preparePostRequest(
				acceptedMediaTypeSparqlResultsXml, ASK_QUERY);		
        Client client = new Client(Protocol.HTTP);        
        Response response = client.handle(request);		
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		Constants.MIME_TYPE_SPARQL_RESULTS_XML);

        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");	
	}

	public void testPostJson() throws IOException {		
		Request request = preparePostRequest(
				acceptedMediaTypeSparqlResultsJson, TITLE_QUERY);		
        Client client = new Client(Protocol.HTTP);        
        Response response = client.handle(request);		
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		Constants.MIME_TYPE_SPARQL_RESULTS_JSON);

        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");	
	}

	public void testPostTextBoolean() throws IOException {		
		Request request = preparePostRequest(
				acceptedMediaTypeTextBoolean, ASK_QUERY);		
        Client client = new Client(Protocol.HTTP);        
        Response response = client.handle(request);		
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		Constants.MIME_TYPE_TEXT_BOOLEAN);
        
        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");	
	}

	public void testPostBooleanTextPlain() throws IOException {
		Request request = preparePostRequest(
				acceptedMediaTypeTextPlain, ASK_QUERY);		
        Client client = new Client(Protocol.HTTP);        
        Response response = client.handle(request);		
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		MediaType.TEXT_PLAIN.getName());
        
        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");			
	}

	public void testPostTupleTextPlain() throws IOException {		
		Request request = preparePostRequest(
				acceptedMediaTypeTextPlain, TITLE_QUERY);		
        Client client = new Client(Protocol.HTTP);        
        Response response = client.handle(request);		
        assertEquals(Status.SUCCESS_OK , response.getStatus());		

        Representation rep = response.getEntity();        
        assertEquals(
        		rep.getMediaType().getName(), 
        		MediaType.TEXT_PLAIN.getName());
        
        // Debug: System.out.println("#####\n" + rep.getText() + "#####\n");			
	}

	private Request prepareGetRequest(List<Preference<MediaType>> mediaTypes, 
			String query) throws IOException {
		String q = URLEncoder.encode(query, "UTF-8");		
		Request request = 
			new Request(Method.GET, serviceAppUrl + "/sparql?query=" + q);        		
        request.getClientInfo().setAcceptedMediaTypes(mediaTypes);
        return request; 
	}

	private Request preparePostRequest(List<Preference<MediaType>> mediaTypes, 
			String query) throws IOException {
		Request request = 
			new Request(Method.POST, serviceAppUrl + "/sparql");		
        String param = "query=" + query;
        request.setEntity(param, MediaType.MULTIPART_FORM_DATA);     
        request.getClientInfo().setAcceptedMediaTypes(mediaTypes);
        return request; 
	}

}