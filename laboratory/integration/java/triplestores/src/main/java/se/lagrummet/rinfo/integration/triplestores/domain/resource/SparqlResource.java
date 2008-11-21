package se.lagrummet.rinfo.integration.triplestores.domain.resource;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.restlet.Context;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;

import se.lagrummet.rinfo.integration.triplestores.ServiceApplication;

public class SparqlResource extends Resource {

    public SparqlResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public boolean allowPost() { 
    	return true; 
    }

    @Override
    public void handleGet() {    	    	
    	// TODO
    	String message = "Get " + getRequest().getAttributes().get("query");
    	getResponse().setEntity(message, MediaType.TEXT_PLAIN);
    }    

    
    @Override
    public void acceptRepresentation(Representation entity) 
    throws ResourceException {

        String query = getRequest().getEntityAsForm().getFirstValue("query");
        if (query == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing query parameter.");
            return;
        }

//    	List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(getRequest());    	    	
//    	if (acceptedMediaTypes.contains("application/sparql-results+xml")) {
//    		
//    	}

        // depending on accepted media type, create corresponding writer...
        
//        SPARQLResultsXMLWriter sparqlWriter;
//        = new SPARQLResultsXMLWriter();
        
        /*
         * TODO: how to handle large results - buffered writer to file?
         * limit to certain size?
         * 
         * how to pipe result to suitable format for delivering results?
         */
        
		Repository repo = ServiceApplication.getRepository();
        try {
        	RepositoryConnection conn = repo.getConnection();
        	try {
        		Query q = conn.prepareQuery(QueryLanguage.SPARQL, query);
      			// q.evaluate(sparqlWriter);
        	}
        	finally {
        		conn.close();
        	}
        }
        catch (OpenRDFException e) {
        	// TODO
        }                
        
        
    	getResponse().setEntity("", MediaType.TEXT_PLAIN);
    	
    }
    
    private List<MediaType> getAcceptedMediaTypes(Request request) {
    	ClientInfo clientInfo = request.getClientInfo();
    	List<Preference<MediaType>> preferences = clientInfo.getAcceptedMediaTypes();
    	ArrayList<MediaType> acceptedMediaTypes = new ArrayList<MediaType>(); 
    	for (Preference<MediaType> p : preferences) {
    		acceptedMediaTypes.add(p.getMetadata());
    	}
    	return acceptedMediaTypes;
    }

}
