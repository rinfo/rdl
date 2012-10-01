package se.lagrummet.rinfo.rdf.repo


import org.apache.commons.configuration.MapConfiguration
import org.openrdf.model.Statement
import org.openrdf.model.impl.StatementImpl
import org.openrdf.model.impl.URIImpl
import spock.lang.Specification
import spock.lang.Unroll

/*
 * TODO: add tests for verifying behaviour of using inference / DT.
 */
public class LocalRepositoryHandlerSpec extends Specification {

  static String TEST_DATA_DIR = "target/sesame-test-data"
  static String TEST_REPO_ID = "rinfo-test"
  
  @Unroll
  def "Test of repositories of different types (#storeType, #inferenceType)"() {
    given: "A new local repository"
    def handler = new LocalRepositoryHandler(TEST_DATA_DIR, TEST_REPO_ID, storeType, inferenceType)
    handler.initialize()
    handler.cleanRepository()
    def conn = handler.repository.connection

    expect: "It should be empty"
    conn.empty
    conn.contextIDs.asList().size() == 0
    conn.namespaces.asList().size() == 0

    when: "Adding a statement"
    String s = "http://example.org/s"
    String p = "http://example.org/p"
    String o = "http://example.org/o"
    Statement st = new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o))
    conn.add(st)

    then: "It should not be empty"
    !conn.empty
    conn.size() == 1

    when: "Cleaning up the repository"
    conn.close()
    handler.cleanRepository()
    conn = handler.getRepository().getConnection()

    then: "The repo should be empty again"
    conn.empty
    conn.contextIDs.asList().size() == 0
    conn.namespaces.asList().size() == 0

    cleanup:
    conn.close()

    where:
    storeType | inferenceType
    "memory"  | null
    "memory"  | "dt"
    "memory"  | "rdfs"
    "native"  | null
  }

  def "Test configuration via properties"() {
    given: "Configuration specifying a local repository"
    def configuration = new MapConfiguration(new HashMap())
    configuration.addProperty "repositoryId", "rinfo-test"
    configuration.addProperty "storeType", "native"
    configuration.addProperty "native.dataDir", "target/sesame-test-data"

    when: "The factory crates the repo"
    def handler = RepositoryHandlerFactory.create(configuration)

    then: "It should be a LocalRepositoryHandler"
    handler.class == LocalRepositoryHandler
  }

}
