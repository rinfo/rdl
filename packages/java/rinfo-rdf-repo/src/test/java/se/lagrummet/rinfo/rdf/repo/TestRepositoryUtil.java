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

    @SuppressWarnings({ "static-access" })
	private void testAddDelete(Configuration config) throws Exception {
    	RepositoryFactory factory = new RepositoryFactory(config); 
		Repository repo = factory.getRepository();
		RepositoryConnection conn = repo.getConnection();
		assertTrue("Expected repository to be empty.", conn.isEmpty());
		String s = "http://example.org/s";
		String p = "http://example.org/p";
		String o = "http://example.org/o";		
		Statement st = new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o));
		conn.add(st);
		assertEquals(1, conn.size());
		RepositoryUtil.cleanRepository();
		assertTrue("Expected repository to be empty.", conn.isEmpty());
		assertEquals(0, conn.getContextIDs().asList().size());		
		assertEquals(0, conn.getNamespaces().asList().size());    	
    }
    
	public void testSesameMemory() throws Exception {		
		Configuration config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
		config.setProperty("repository.name", "rinfo-test");
		config.setProperty("backend", "memory");
		testAddDelete(config);
	}
	
	public void testSesameNative() throws Exception {
		Configuration config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
		config.setProperty("repository.name", "rinfo-test");
		config.setProperty("backend", "native");
		testAddDelete(config);		
	}

}
