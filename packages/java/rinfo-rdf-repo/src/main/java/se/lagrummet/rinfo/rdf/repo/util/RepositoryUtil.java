package se.lagrummet.rinfo.rdf.repo.util;

import org.apache.commons.configuration.Configuration;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

import se.lagrummet.rinfo.rdf.repo.RepositoryFactory;

/**
 * Utility class for setup, clean and removal of repositories.
 * 
 * @author msher
 */
public class RepositoryUtil {

	/**
	 * Setup (initialises) a repository if it is not already existing.
	 * Uses the default configuration to specify the repository.
	 * 
	 * @throws Exception
	 */
	public static void setupRepository() throws Exception {
		setupRepository(null);
	}

	/**
	 * Setup (initialises) a repository if it is not already existing. 
	 * Uses the given configuration to specify the repository.
	 * 
	 * @param config
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	public static void setupRepository(Configuration config) 
	throws Exception {		
		RepositoryFactory factory = null;
		try {
			if (config != null) {
				factory = new RepositoryFactory(config); 
			} else {
				factory = new RepositoryFactory(); 
			}			
			factory.getRepository();
		} finally {
			if (factory != null) {
				factory.shutDown();
			}
		}
	}	

	/**
	 * Removes all content in the repository.
	 * Uses the default configuration to specify the repository.
	 * 
	 * @throws Exception
	 */
	public static void cleanRepository() throws Exception {
		cleanRepository(null);
	}

	/**
	 * Removes all content in the repository.
	 * Uses the given configuration to specify the repository.
	 * 
	 * @param config
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	public static void cleanRepository(Configuration config) 
	throws Exception {		
		RepositoryFactory factory = null;
		RepositoryConnection conn = null;
		try {
			if (config != null) {
				factory = new RepositoryFactory(config); 
			} else {
				factory = new RepositoryFactory(); 
			}
			Repository repo;
			repo = factory.getRepository();
			conn = repo.getConnection();
			conn.clear();
			conn.clearNamespaces();
		} finally {
			if (conn != null && conn.isOpen()) {
				conn.close();
			}			
			if (factory != null) {
				factory.shutDown();
			}
		}
	}	

	/**
	 * Remove repository from the repository manager. All content in repository
	 * is also removed.  
	 * 
	 * @param config
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	public static void removeRemoteRepository(Configuration config) 
	throws Exception {		
		RepositoryFactory factory = null;
		try {
			if (config != null) {
				factory = new RepositoryFactory(config); 
			} else {
				factory = new RepositoryFactory(); 
			}
			factory.removeRemoteRepository();
		} finally {
			if (factory != null) {
				factory.shutDown();
			}
		}
	}

}
