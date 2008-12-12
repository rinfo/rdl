package se.lagrummet.rinfo.rdf.repo;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

import junit.framework.TestCase;

 
public class TestRepositoryUtil extends TestCase {
		
    private static final String PROPERTIES_FILE_NAME = "rinfo-rdf-repo.properties";

    /*
     * TODO: add tests for verifying behaviour of using inference / DT.
     */
    
	public void testLocalSesameMemory() throws Exception {
		testAddDelete("sesame", "memory", true);
	}

	public void testLocalSesameNative() throws Exception {
		testAddDelete("sesame", "native", true);
	}

	public void testRemoteSesameMemory() throws Exception {
		testAddDelete("sesame", "memory", false);
	}

	public void testRemoteSesameNative() throws Exception {
		testAddDelete("sesame", "native", false);
	}

	@SuppressWarnings("static-access")
	private void testAddDelete(String tripleStore, String backend, boolean isLocalRepository) 
	throws Exception {
		Configuration config = getConfiguration(tripleStore, backend);
		RepositoryFactory factory = new RepositoryFactory(config);
		RepositoryConnection conn = null;
		try {
			Repository repo;
			if (isLocalRepository) {
				repo = factory.getLocalRepository();
			} else {
				repo = factory.getRemoteRepository();
			}
			conn = repo.getConnection();			
			assertTrue("Expected repository to be empty.", conn.isEmpty());
			String s = "http://example.org/s";
			String p = "http://example.org/p";
			String o = "http://example.org/o";		
			Statement st = new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o));
			conn.add(st);			
			assertEquals(1, conn.size());
			conn.close();
			
			if (isLocalRepository) {
				RepositoryUtil.cleanLocalRepository(config);				
			} else {
				RepositoryUtil.cleanRemoteRepository(config);								
			}
			
			if (isLocalRepository) {
				repo = factory.getLocalRepository();
			} else {
				repo = factory.getRemoteRepository();
			}
			conn = repo.getConnection();
			assertTrue("Expected repository to be empty.", conn.isEmpty());
			assertEquals(0, conn.getContextIDs().asList().size());		
			assertEquals(0, conn.getNamespaces().asList().size());    				
		} finally {
			if (conn != null) {
				conn.close();
			}
			if (factory != null) {
				factory.shutDown();
			}
		}
    }

	private Configuration getConfiguration(String tripleStore, String backend) 
	throws Exception  {
		Configuration config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
		String testRepoId = config.getString("test.repository.id");
		config.setProperty("repository.id", testRepoId);
		config.setProperty("triple.store", tripleStore);
		config.setProperty("backend", backend);
    	return config;
	}

}
