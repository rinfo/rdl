package se.lagrummet.rinfo.rdf.repo;

import java.util.*;

import junit.framework.TestCase;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.NotImplementedException;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;


public class RepositoryHandlerTest extends TestCase {

    static List<String> TEST_PROPERTIES_FILES = Arrays.asList(
            "test-rdf-repo-native.properties");
    // TODO: see integration tests note below.
    //        "test-rdf-repo-native.properties",
    //        "test-rdf-repo-http.properties");

    static String TEST_DATA_DIR = "target/sesame-test-data";
    static String TEST_REMOTE_SERVER_URL = "http://localhost:8080/openrdf-sesame";
    static String TEST_REPO_ID = "rinfo-test";

    /*
     * TODO: add tests for verifying behaviour of using inference / DT.
     */

    public void testLocalSesameMemory() throws Exception {
        testAddDelete(createLocalRepositoryHandler("memory", null));
    }

    public void testLocalSesameMemoryRdfs() throws Exception {
        testAddDelete(createLocalRepositoryHandler("memory", "rdfs"));
    }

    public void testLocalSesameMemoryDt() throws Exception {
        testAddDelete(createLocalRepositoryHandler("memory", "dt"));
    }

    public void testLocalSesameNative() throws Exception {
        testAddDelete(createLocalRepositoryHandler("native", null));
    }

    // TODO: as separate integration tests, or start a sesame server in-process
    //
    //public void testRemoteSesameMemory() throws Exception {
    //    testAddDelete(createRemoteRepositoryHandler("memory", null));
    //}

    //public void testRemoteSesameNative() throws Exception {
    //    testAddDelete(createRemoteRepositoryHandler("native", null));
    //}

    public void testConfiguredViaProperties() throws Exception {
        for (String propsFilePath : TEST_PROPERTIES_FILES) {
            testAddDelete(
                    RepositoryHandlerFactory.create(
                        new PropertiesConfiguration(propsFilePath))
                );
        }
    }


    private void testAddDelete(RepositoryHandler repoHandler) throws Exception {
        RepositoryConnection conn = null;
        repoHandler.initialize();
        repoHandler.cleanRepository();
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
            try {
                repoHandler.removeRepository();
            } catch (NotImplementedException e) {
                // TODO: not supported by LocalRepositoryHandler
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

    private RepositoryHandler createLocalRepositoryHandler(
            String storeType, String inferenceType) throws Exception  {
        return new LocalRepositoryHandler(
                TEST_REPO_ID, storeType, inferenceType, TEST_DATA_DIR);
    }

    private RepositoryHandler createRemoteRepositoryHandler(
            String storeType, String inferenceType) throws Exception  {
        return new RemoteRepositoryHandler(
                TEST_REPO_ID, storeType, inferenceType, TEST_REMOTE_SERVER_URL);
    }

}
