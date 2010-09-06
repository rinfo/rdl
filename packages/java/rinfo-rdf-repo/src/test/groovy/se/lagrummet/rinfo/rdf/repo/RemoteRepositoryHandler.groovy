package se.lagrummet.rinfo.rdf.repo

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import org.openrdf.model.impl.URIImpl
import org.openrdf.model.impl.StatementImpl
import org.openrdf.model.Statement
import org.openrdf.repository.RepositoryConnection
import org.apache.commons.configuration.MapConfiguration
import spock.lang.Specification

//TODO Needs to be properly implemented as an integration test?
class RemoteRepositoryHandlerSpec extends Specification {

  static List<String> TEST_PROPERTIES_FILES = Arrays.asList("test-rdf-repo-http.properties");
  static String TEST_REMOTE_SERVER_URL = "http://localhost:8080/openrdf-sesame";
  static String TEST_REPO_ID = "rinfo-test";

  def "Test configuration via properties"() {
    given: "Configuration specifying a remote repository"
    def configuration = new MapConfiguration(new HashMap())
    configuration.addProperty "repositoryId", "rinfo-test"
    configuration.addProperty "storeType", "native"
    configuration.addProperty "inferenceType", "rdfs"
    configuration.addProperty "remote.serverUrl", "http://localhost:8080/openrdf-sesame"

    when: "The factory crates the repo"
    def handler = RepositoryHandlerFactory.create(configuration)

    then: "It should be a RemoteRepositoryHandler"
    handler.class == RemoteRepositoryHandler
  }

  // TODO: as separate integration tests, or start a sesame server in-process
  //
  //public void testRemoteSesameMemory() throws Exception {
  //    testAddDelete(createRemoteRepositoryHandler("memory", null));
  //}

  //public void testRemoteSesameNative() throws Exception {
  //    testAddDelete(createRemoteRepositoryHandler("native", null));
  //}

  private void testAddDelete(RepositoryHandler repoHandler) throws Exception {

    RepositoryConnection conn;
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

  private RepositoryHandler createRemoteRepositoryHandler(
  String storeType, String inferenceType) throws Exception {
    def handler = new RemoteRepositoryHandler(TEST_REMOTE_SERVER_URL, TEST_REPO_ID,
            storeType, inferenceType)
    handler.initialize();
    handler.cleanRepository();
    return handler;
  }

}
