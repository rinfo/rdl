package se.lagrummet.rinfo.rdf.repo;

import org.apache.commons.configuration.Configuration;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

public class RepositoryUtil {

	public static void cleanLocalRepository() throws Exception {
		cleanRepository(null, true);
	}

	public static void cleanLocalRepository(Configuration config) throws Exception {
		cleanRepository(config, true);
	}

	public static void cleanRemoteRepository() throws Exception {
		cleanRepository(null, false);
	}

	public static void cleanRemoteRepository(Configuration config) throws Exception {
		cleanRepository(config, false);
	}

	public static void setupLocalRepository() throws Exception {
		setupRepository(null, true);
	}

	public static void setupLocalRepository(Configuration config) throws Exception {
		setupRepository(config, true);
	}

	public static void setupRemoteRepository() throws Exception {
		setupRepository(null, false);
	}

	public static void setupRemoteRepository(Configuration config) throws Exception {
		setupRepository(config, false);
	}

	
	@SuppressWarnings("static-access")
	private static void cleanRepository(Configuration config, boolean isLocal) 
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
			if (isLocal) {
				repo = factory.getLocalRepository();
			} else {				
				repo = factory.getRemoteRepository();
			}
			conn = repo.getConnection();
			conn.clear();
			conn.clearNamespaces();
		} finally {
			if (conn != null) {
				conn.close();
			}			
			if (factory != null) {
				factory.shutDown();
			}
		}
	}	

	@SuppressWarnings("static-access")
	private static void setupRepository(Configuration config, boolean isLocal) 
	throws Exception {		
		RepositoryFactory factory = null;
		try {
			if (config != null) {
				factory = new RepositoryFactory(config); 
			} else {
				factory = new RepositoryFactory(); 
			}			
			if (isLocal) {
				factory.getLocalRepository();
			} else {				
				factory.getRemoteRepository();
			}
		} finally {
			if (factory != null) {
				factory.shutDown();
			}
		}
	}	

}
