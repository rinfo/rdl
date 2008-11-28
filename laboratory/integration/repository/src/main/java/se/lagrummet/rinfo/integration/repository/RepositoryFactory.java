package se.lagrummet.rinfo.integration.repository;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.openrdf.repository.Repository;
import org.openrdf.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.repository.sail.SailRepository;

public class RepositoryFactory {

    private static final String PROPERTIES_FILE_NAME = "repository.properties";

    private static final List<String> SUPPORTED_TRIPLE_STORES = Arrays.asList(
			"sesame", "jena", "mulgara", "swiftowlim", "openlink-virtuoso");

	private static Repository repository;

	/*
	 * TODO:
	 * Constructor without config
	 * 
	 * public init method
	 * call init in getRepo if not yet initialized 
	 * 
	 * shutdown?
	 */
    
	public RepositoryFactory(Configuration config) throws Exception {

		validateConfiguration(config);

		String store = config.getString("triple.store").toLowerCase();
		String backend = config.getString("backend").toLowerCase();
		
		if (store.equals("sesame")) {

			boolean inference = config.getBoolean("inference");
			boolean inferenceDT = config.getBoolean("inference.direct.type");

			if (inference) {
				repository = new SailRepository(
						new ForwardChainingRDFSInferencer(new MemoryStore()));								
			} else if (inferenceDT) {
				repository = new SailRepository(
						new DirectTypeHierarchyInferencer(new MemoryStore()));								
			} else {
				repository = new SailRepository(new MemoryStore());				
			}

			repository.initialize();
		}
	}

	public static synchronized Repository getRepository() {
		return repository;
	}
	
	private static void validateConfiguration(Configuration config) throws Exception {		
		String store = config.getString("triple.store").toLowerCase();
		boolean inference = config.getBoolean("inference");
		boolean inferenceDT = config.getBoolean("inference.direct.type");
		
		if (!SUPPORTED_TRIPLE_STORES.contains(store)) {
			throw new Exception("Unsupported triple store: " + store);			
		}
		
		if (inference && inferenceDT) {
			throw new Exception("Conflicting properties. 'inference' and " 
					+ "'inference.direct.type' cannot both be true");				
		}		
	}
}
