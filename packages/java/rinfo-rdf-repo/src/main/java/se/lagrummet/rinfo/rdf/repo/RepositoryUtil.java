package se.lagrummet.rinfo.rdf.repo;


import org.apache.commons.configuration.Configuration;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

public class RepositoryUtil {

	public static void cleanRepository() throws Exception {
		cleanRepository(null);
	}

	@SuppressWarnings("static-access")
	public static void cleanRepository(Configuration config) throws Exception {
		RepositoryConnection conn = null;
		try {
			Repository repo; 
			if (config != null) {
				RepositoryFactory rf = new RepositoryFactory(config); 
				repo = rf.getRepository();				
			} else {
				repo = RepositoryFactory.getRepository();				
			}
			conn = repo.getConnection();
			conn.clear();
			conn.clearNamespaces();

		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}

	public static void setupRepository() throws Exception {
		setupRepository(null);
	}

	@SuppressWarnings("static-access")
	public static void setupRepository(Configuration config) throws Exception {
		if (config != null) {
			RepositoryFactory rf = new RepositoryFactory(config);
			rf.getRepository();				
		} else {
			RepositoryFactory.getRepository();				
		}
	}

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				// TODO
				System.out.println("Usage ...");
			} else if (args[0].equals("clean")) {
				cleanRepository();
			} else if (args[0].equals("setup")) {
				setupRepository();
			} else {
				// TODO
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
