package se.lagrummet.rinfo.rdf.repo;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;

import se.lagrummet.rinfo.rdf.repo.util.ConfigurationUtil;
import se.lagrummet.rinfo.rdf.repo.util.RepositoryUtil;

 
public class TestRepositoryUtil extends TestCase {
		
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
	private void testAddDelete(String tripleStore, String backend, boolean useLocalRepository) 
	throws Exception {
		Configuration config = getConfiguration(tripleStore, backend, useLocalRepository);
		RepositoryFactory factory = new RepositoryFactory(config);
		RepositoryConnection conn = null;
		try {
			conn = factory.getRepository().getConnection();			
			assertTrue("Expected repository to be empty.", conn.isEmpty());
			String s = "http://example.org/s";
			String p = "http://example.org/p";
			String o = "http://example.org/o";		
			Statement st = new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o));
			conn.add(st);			
			assertEquals(1, conn.size());
			conn.close();

			RepositoryUtil.cleanRepository(config);				
			conn = factory.getRepository().getConnection();			
			assertTrue("Expected repository to be empty.", conn.isEmpty());
			assertEquals(0, conn.getContextIDs().asList().size());		
			assertEquals(0, conn.getNamespaces().asList().size()); 
			// clean up
			if (!useLocalRepository) {
				RepositoryUtil.removeRemoteRepository(config);				
			}
		} finally {
			if (conn != null && conn.isOpen()) {
				conn.close();
			}
			if (factory != null) {
				factory.shutDown();
			}
		}
    }
	
	private Configuration getConfiguration(String tripleStore, String backend, 
			boolean useLocalRepository) throws Exception  {
		Configuration config = ConfigurationUtil.getDefaultConfiguration();
		String testRepoId = config.getString("test.repository.id");
		config.setProperty("repository.id", testRepoId);
		config.setProperty("triple.store", tripleStore);
		config.setProperty("backend", backend);
		config.setProperty("use.local.repository", useLocalRepository);
    	return config;
	}

}
