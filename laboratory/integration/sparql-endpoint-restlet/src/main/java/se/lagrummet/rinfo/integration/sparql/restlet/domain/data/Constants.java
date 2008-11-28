package se.lagrummet.rinfo.integration.sparql.restlet.domain.data;

import org.restlet.data.MediaType;

public class Constants {
	
	public static final String MIME_TYPE_SPARQL_RESULTS_XML = 
		"application/sparql-results+xml";
	
	public static final String MIME_TYPE_SPARQL_RESULTS_JSON = 
		"application/sparql-results+json";
	
	public static final String MIME_TYPE_TEXT_BOOLEAN = "text/boolean";

	public static final MediaType MEDIA_TYPE_SPARQL_RESULTS_XML = 
		new MediaType(MIME_TYPE_SPARQL_RESULTS_XML);	
	
	public static final MediaType MEDIA_TYPE_SPARQL_RESULTS_JSON = 
		new MediaType(MIME_TYPE_SPARQL_RESULTS_JSON);	
	
	public static final MediaType MEDIA_TYPE_TEXT_BOOLEAN = 
		new MediaType(MIME_TYPE_TEXT_BOOLEAN);

}
