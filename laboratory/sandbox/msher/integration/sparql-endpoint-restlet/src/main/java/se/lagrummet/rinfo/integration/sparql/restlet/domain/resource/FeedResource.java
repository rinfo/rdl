package se.lagrummet.rinfo.integration.sparql.restlet.domain.resource;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public class FeedResource extends Resource {

    public FeedResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public boolean allowPost() { 
    	return true; 
    }

    /*
     * TODO: handle get, return url of latest feed collected / actual atom feed?
     */
//    @Override
//    public void handleGet() {    	    	
//    	String message = "Get " + getRequest().getAttributes().get("url");
//    	getResponse().setEntity(message, MediaType.TEXT_PLAIN);
//    }    
    
    @Override
    public void handlePost() {
    	String message = "Post " + getRequest().getAttributes().get("url");
    	getResponse().setEntity(message, MediaType.TEXT_PLAIN);
    	
    	
//        String feedUrl = request.getEntityAsForm().getFirstValue("feed")
//        if (feedUrl == null) {
//            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
//                    "Missing feed parameter.")
//            return
//        }
//        logger.info("ServiceApplication: Scheduling collect of <${feedUrl}>.")
//        triggerFeedCollect(new URL(feedUrl))
//        response.setEntity("Scheduled collect of <${feedUrl}>.", MediaType.TEXT_PLAIN)
    }

//    boolean triggerFeedCollect(URL feedUrl) {
//        // TODO: verify source of request and/or feedUrl
//        // FIXME: error handling.. (report and/or (public) log)
//
//        def repo = (Repository) getContext().getAttributes().get(
//                ServiceApplication.RDF_REPO_CONTEXT_KEY)
//                
//        def pool = (ExecutorService) getContext().getAttributes().get(
//                ServiceApplication.THREAD_POOL_CONTEXT_KEY) 
//
//        def logger = (Logger) getContext().getAttributes().get(
//        		ServiceApplication.LOGGER_CONTEXT_KEY) 
//        		
//		pool.execute({
//            logger.info("Beginning collect of <${feedUrl}>.")            
//            // TODO:IMPROVE: Ok to make a new instance for each request? Shouldn't
//            // be so expensive, and isolates it (shouldn't be app-global..)
//            def rdfStoreLoader = new SesameLoader(repo)
//            rdfStoreLoader.readFeed(feedUrl)
//            logger.info("Completed collect of <${feedUrl}>.")                
//        })
//        return true
//    }

    
}
