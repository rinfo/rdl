package se.lagrummet.rinfo.integration.triplestores.domain.resource;

import java.net.URL;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import se.lagrummet.rinfo.integration.triplestores.ServiceApplication;

public class RdfResource extends Resource {

	private static final Logger log = Logger.getLogger(RdfResource.class);

    public RdfResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public boolean allowPost() { 
    	return true; 
    }

    @Override
    public void handleGet() {
    	String message = "Get " + getRequest().getAttributes().get("url");
    	getResponse().setEntity(message, MediaType.TEXT_PLAIN);
    }    
	
    @Override
    public void handlePost() {    	    	
        String rdfUrl = getRequest().getEntityAsForm().getFirstValue("url");
        if (rdfUrl == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing URL parameter.");
            return;
        }

		Repository repo = ServiceApplication.getRepository();
		RepositoryConnection conn = null;
		
		try {
			URL url = new URL(rdfUrl);
			conn = repo.getConnection();
			conn.add(url, null, RDFFormat.RDFXML);				        
			getResponse().setStatus(Status.SUCCESS_OK);
			
		} catch (Exception e) {
			log.error("Failed to load RDF to repository.", e);
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.toString());
            return;
            
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (RepositoryException e) {
					log.error("Failed to close connection to repository.", e);
				}					
			}
		}
    }
    
}
