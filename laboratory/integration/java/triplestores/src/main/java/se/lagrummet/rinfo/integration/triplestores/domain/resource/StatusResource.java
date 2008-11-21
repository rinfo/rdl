package se.lagrummet.rinfo.integration.triplestores.domain.resource;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import se.lagrummet.rinfo.integration.triplestores.ServiceApplication;

public class StatusResource extends Resource {

    public StatusResource(Context context, Request request, Response response) {
        super(context, request, response);

        setModifiable(false);

        getVariants().add(new Variant(MediaType.TEXT_XML));
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        
    	if (MediaType.TEXT_XML.equals(variant.getMediaType())) {
    		
    		String msg = "";
    		Repository repo = ServiceApplication.getRepository();
    		RepositoryConnection conn = null;
    		try {
    			conn = repo.getConnection(); 
				long n = conn.size();				
				msg += "\nRepository: " + repo.getClass();
				msg += "\nStatements in repository: " + n;				
			} catch (RepositoryException e) {
				// TODO log it
				msg += "\nUnable to connect to repository: " + e.toString();
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (RepositoryException e) {
						// TODO log it
						msg += "\nUnable to close connection to repository: " + e.toString();
					}					
				}
			}
    		
        	Representation repr = new StringRepresentation(msg, MediaType.TEXT_PLAIN);
        	return repr;
        }

        return null;
    }

	
}
