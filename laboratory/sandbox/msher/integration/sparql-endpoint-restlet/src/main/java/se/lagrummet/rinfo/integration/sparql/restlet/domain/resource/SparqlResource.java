package se.lagrummet.rinfo.integration.sparql.restlet.domain.resource;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.BooleanQueryResultWriter;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.query.resultio.text.BooleanTextWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import se.lagrummet.rinfo.integration.sparql.restlet.ServiceApplication;
import se.lagrummet.rinfo.integration.sparql.restlet.domain.data.Constants;
import se.lagrummet.rinfo.integration.sparql.restlet.util.PageUtils;

/**
 * SPARQL end-point.
 * 
 * Handles queries sent by GET and POST with the parameter 'query'.  
 * 
 * @author msher
 */
public class SparqlResource extends Resource {

	private static final Logger log = Logger.getLogger(SparqlResource.class);	
	private static String variantNames;


	public SparqlResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		getVariants().add(new Variant(Constants.MEDIA_TYPE_SPARQL_RESULTS_XML));
		getVariants().add(new Variant(Constants.MEDIA_TYPE_SPARQL_RESULTS_JSON));
		getVariants().add(new Variant(Constants.MEDIA_TYPE_TEXT_BOOLEAN));
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		
		variantNames = "";
		for (Variant v : getVariants()) {
			variantNames += v.getMediaType().getName() + ", ";
		}
		variantNames = StringUtils.removeEnd(variantNames, ", ");		
	}

	@Override
	public boolean allowPost() { 
		return true; 
	}

	@Override
	public void handleGet() {		
		if (isPreferredVariantSupported()) {
			String params = getRequest().getResourceRef().getRemainingPart();		
			Map<String, String> map = PageUtils.parsePageParameters(params); 		
			String query = map.get("query");			
			handleSparqlQuery(query);	
		}
	}    
		
	@Override
	public void handlePost() {
		if (isPreferredVariantSupported()) {
			String query = getRequest().getEntityAsForm().getFirstValue("query");
			handleSparqlQuery(query);
		}
	}	

	/**
	 * Determine if the client's preferred variant is supported. If not, set
	 * response status to indicate error 406 - Not Acceptable.
	 * 
	 * @return If the client's preferred variant is supported
	 */
	private boolean isPreferredVariantSupported() {
		Variant preferredVariant = getPreferredVariant();		
		if (preferredVariant == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE,
			"This resource is not capable of delivering content in the " 
					+ "requested media type. Please use one of the following: " 
					+ variantNames);
			return false;
		}
		return true;
	}
	
	/**
	 * Parse and evaluate a SPARQL query, set results as a FileRepresentation
	 * in the response. 
	 *  
	 * @param query
	 */
	private void handleSparqlQuery(String query) {
		
		if (StringUtils.isEmpty(query)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
			"Missing query parameter.");
			return;
		}
		
		query = query.toLowerCase();
		boolean isTupleQuery = query.contains("select") || query.contains("construct");
		
		File result = null;
		String fileName = "rinfo-query-result-" + System.currentTimeMillis();
		MediaType resultType = null;
		BufferedOutputStream output = null;
		TupleQueryResultHandler tupleResultHandler = null;		
		BooleanQueryResultWriter booleanResultWriter = null;
		
		try {
			String variantName = getPreferredVariant().getMediaType().getName();
			String dir = ServiceApplication.getTempDir(); 
			
			if (variantName.equals(Constants.MIME_TYPE_SPARQL_RESULTS_XML)) {				
				resultType = Constants.MEDIA_TYPE_SPARQL_RESULTS_XML;
				result = new File(dir, fileName + ".xml");				
				output = new BufferedOutputStream(new FileOutputStream(result));
				
				if (isTupleQuery) {
					tupleResultHandler = new SPARQLResultsXMLWriter(output);					
				} else {
					booleanResultWriter = new SPARQLBooleanXMLWriter(output);
				}

			} else if (variantName.equals(Constants.MIME_TYPE_SPARQL_RESULTS_JSON)) {
				resultType = Constants.MEDIA_TYPE_SPARQL_RESULTS_JSON;
				result = new File(dir, fileName + ".json");				
				output = new BufferedOutputStream(new FileOutputStream(result));
				tupleResultHandler = new SPARQLResultsJSONWriter(output);
				
			} else if (variantName.equals(Constants.MIME_TYPE_TEXT_BOOLEAN)) {				
					resultType = Constants.MEDIA_TYPE_TEXT_BOOLEAN;
					result = new File(dir, fileName + ".txt");				
					output = new BufferedOutputStream(new FileOutputStream(result));
					booleanResultWriter = new BooleanTextWriter(output);
				
			} else {
				resultType = MediaType.TEXT_PLAIN;
				result = new File(dir, fileName + ".txt");				
				output = new BufferedOutputStream(new FileOutputStream(result));
				if (isTupleQuery) {
					tupleResultHandler = new SPARQLResultsXMLWriter(output);					
				} else {
					booleanResultWriter = new BooleanTextWriter(output);
				}
			}

		} catch (IOException e) {
			log.fatal("Could not create temp file for SPARQL query result.", e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
		}          
		
		RepositoryConnection conn = null;
		try {
			Repository repo = ServiceApplication.getRepository();
			conn = repo.getConnection();        	        			
			
			if (isTupleQuery) {				
				TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
				q.evaluate(tupleResultHandler);
			} else {
				BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, query);
				booleanResultWriter.write(q.evaluate());
			}
			
		} catch (IllegalArgumentException e) {
			log.info("Supplied query is not a tuple query: " + query, e);
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
			"Supplied query is not a tuple query.");
			return;
			
		} catch (MalformedQueryException e) {
			log.info("Supplied query is malformed: " + query, e);
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
			"Supplied query is malformed.");
			return;

		} catch (QueryEvaluationException e) {
			log.info("Evaluation of the query failed for query: " + query, e);
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
			"Evaluation of the query failed.");
			return;

		} catch (TupleQueryResultHandlerException e) {
			log.info("Evaluation of the query failed for query: : " + query, e);
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
			"Evaluation of the query failed.");
			return;

		} catch (RepositoryException e) {
			log.error("Unexpected exception in repository connection.", e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
			
		} catch (IOException e) {
			log.fatal("Could not write to temp file for SPARQL query result.", e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
						
		} finally {
			boolean errorInFinallyBlock = false;			
			try {
				output.flush();
				output.close();
			} catch (IOException e) {
				errorInFinallyBlock = true;
				log.fatal("Could not close temp file for SPARQL query result.", e);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (RepositoryException e) {
					errorInFinallyBlock = true;
					log.fatal("Could not close repository connection.", e);
				} 
			}
			if (errorInFinallyBlock) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				return;
			}
		}

		FileRepresentation rep = new FileRepresentation(result, resultType);		
		getResponse().setEntity(rep);		
	}
}
