package se.lagrummet.rinfo.rdf.repo;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;


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

    private void testAddDelete(String tripleStore, String backend, boolean useLocalRepository)
    throws Exception {
        RepositoryHandler repoHandler = createRepositoryHandler(
                tripleStore, backend, useLocalRepository);
        repoHandler.initialize();
        repoHandler.cleanRepository();

        RepositoryConnection conn = null;
        try {
            conn = repoHandler.getRepository().getConnection();
            assertTrue("Expected repository to be empty.", conn.isEmpty());
            String s = "http://example.org/s";
            String p = "http://example.org/p";
            String o = "http://example.org/o";
            Statement st = new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o));
            conn.add(st);
            assertEquals(1, conn.size());
            conn.close();

            repoHandler.cleanRepository();
            conn = repoHandler.getRepository().getConnection();
            assertTrue("Expected repository to be empty.", conn.isEmpty());
            assertEquals(0, conn.getContextIDs().asList().size());
            assertEquals(0, conn.getNamespaces().asList().size());
            // clean up
            if (!useLocalRepository) {
                repoHandler.removeRepository();
            }
        } finally {
            if (conn != null && conn.isOpen()) {
                conn.close();
            }
            if (repoHandler != null) {
                repoHandler.shutDown();
            }
        }
    }

    private RepositoryHandler createRepositoryHandler(String tripleStore, String backend,
            boolean useLocalRepository) throws Exception  {
        return new RepositoryHandler(tripleStore, backend,
                useLocalRepository,
                "http://localhost:8080/openrdf-sesame",
                "target/sesame-test-data",
                "rinfo-test",
                false, false);
    }

}
