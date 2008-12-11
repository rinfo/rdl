package se.lagrummet.rinfo.rdf.repo;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.openrdf.repository.Repository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.nativerdf.NativeStore;
import org.openrdf.repository.sail.SailRepository;

public class RepositoryFactory {

    private static final String PROPERTIES_FILE_NAME = "rinfo-rdf-repo.properties";

    private static final List<String> SUPPORTED_TRIPLE_STORES = Arrays.asList(
			"sesame");
//    "sesame", "jena", "mulgara", "swiftowlim", "openlink-virtuoso");

	private static Repository repository;


	public RepositoryFactory() throws Exception {
		this(getDefaultConfiguration());
	}

	public RepositoryFactory(Configuration config) throws Exception {
		init(config);
	}

	public static synchronized Repository getRepository() throws Exception {
		if (repository == null) {
			init(getDefaultConfiguration());
		}
		return repository;
	}
	
	private static Configuration getDefaultConfiguration() 
	throws ConfigurationException {
		return new PropertiesConfiguration(PROPERTIES_FILE_NAME);					
	}
	
	private static void init(Configuration config) throws Exception {

		validateConfiguration(config);

		String store = config.getString("triple.store").toLowerCase();
		String backend = config.getString("backend").toLowerCase();
		String dataDir = config.getString("data.dir");
		String repoName = config.getString("repository.name");
		
		if (store.equals("sesame")) {

			boolean inference = config.getBoolean("inference");
			boolean inferenceDT = config.getBoolean("inference.direct.type");
			
			Sail sail = null;			
			if (backend.equals("memory")) {
				sail = new MemoryStore();
			} else if (backend.equals("native")) {
				sail = new NativeStore(new File(dataDir + "/" + repoName));				
			}
			
			if (inference) {				
				repository = new SailRepository(
						new ForwardChainingRDFSInferencer(sail));								
			} else if (inferenceDT) {
				repository = new SailRepository(
						new DirectTypeHierarchyInferencer(sail));								
			} else {
				repository = new SailRepository(sail);				
			}

			repository.initialize();
		}
		
	}
	
	private static void validateConfiguration(Configuration config) throws Exception {		
		String store = config.getString("triple.store").toLowerCase();
		String backend = config.getString("backend").toLowerCase();
		String dataDir = config.getString("data.dir");
		String repoName = config.getString("repository.name");
		boolean inference = config.getBoolean("inference");
		boolean inferenceDT = config.getBoolean("inference.direct.type");
		
		if (StringUtils.isEmpty(store)) {
			throw new Exception("Missing property 'store'.");						
		}		
		if (StringUtils.isEmpty(backend)) {
			throw new Exception("Missing property 'backend'.");						
		}
		if (backend.equals("native") && StringUtils.isEmpty(dataDir)) {
			throw new Exception("Missing property 'data.dir'.");									
		}
		if (backend.equals("native") && StringUtils.isEmpty(repoName)) {
			throw new Exception("Missing property 'repository.name'.");									
		}		
		if (!SUPPORTED_TRIPLE_STORES.contains(store)) {
			throw new Exception("Unsupported triple store: " + store);			
		}		
		if (inference && inferenceDT) {
			throw new Exception("Conflicting properties. 'inference' and " 
					+ "'inference.direct.type' cannot both be true");				
		}

	}
}
