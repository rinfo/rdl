package se.lagrummet.rinfo.integration.sparql.restlet;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.Router;

import se.lagrummet.rinfo.integration.sparql.restlet.domain.resource.FeedResource;
import se.lagrummet.rinfo.integration.sparql.restlet.domain.resource.RdfResource;
import se.lagrummet.rinfo.integration.sparql.restlet.domain.resource.SparqlResource;
import se.lagrummet.rinfo.integration.sparql.restlet.domain.resource.StatusResource;
import se.lagrummet.rinfo.integration.repository.RepositoryFactory;

public class ServiceApplication extends Application {

	private static final Logger log = Logger.getLogger(ServiceApplication.class);
    private static final String PROPERTIES_FILE_NAME = 
    	"ServiceApplication.properties";
    
	private static RepositoryFactory repositoryFactory;
	private static Configuration config;
	private static String tempDir;
	private static String tempDirUri;
	
	
    public ServiceApplication() {
    	try {
    		
			config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);			
			repositoryFactory = new RepositoryFactory(config);

			tempDir = config.getString("tmp.dir");
			File f = new File(tempDir);
			f.mkdir();
			tempDirUri = f.toURI().toString();
			
		} catch (Exception e) {
			log.fatal(e);
			System.exit(1);
		}    	
    }
    
	@Override
	public synchronized Restlet getRoot() {
		
		Router router = new Router(getContext());
		router.attach("/feed", FeedResource.class);
		router.attach("/rdf", RdfResource.class);		
		router.attach("/sparql", SparqlResource.class);
//		router.attach("/sparql/{query}", SparqlResource.class);
//		router.attach("/serql/{query}", SparqlResource.class);
				
		router.attach("/status", StatusResource.class);
		return router;
	}
	
    @Override
    public void stop() {
        try {
			super.stop();			
		} catch (Exception e) {
			log.error(e);
		}
        try {
			getRepository().shutDown();			
		} catch (RepositoryException e) {
			log.error(e);
		}
    }

	@SuppressWarnings("static-access")
	public static synchronized Repository getRepository() {
		return repositoryFactory.getRepository();
	}
	
	public static String getTripleStoreType() {
		return config.getString("triple.store");
	}
	
	public static String getBackendType() {
		return config.getString("backend");
	}

	public static String getTempDir() {
		return tempDir;
	}

	public static String getTempDirUri() {
		return tempDirUri;
	}
	
}
