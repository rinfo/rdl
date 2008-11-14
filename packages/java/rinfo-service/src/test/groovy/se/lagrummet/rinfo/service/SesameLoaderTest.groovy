package se.lagrummet.rinfo.service

import org.junit.*
import static org.junit.Assert.*

import org.restlet.*
import org.restlet.data.Protocol

import org.openrdf.model.vocabulary.RDFS
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore

import se.lagrummet.rinfo.base.rdf.RDFUtil


class SesameLoaderTest {

    static loader
    static repo
    static component
    static testHttpPort = 9991
    def baseUrl = "http://localhost:${testHttpPort}"

    @BeforeClass
    static void setupClass() {
        repo = RDFUtil.createMemoryRepository()
        loader = new SesameLoader(repo)
        component = new Component()
        component.with {
            servers.add(Protocol.HTTP, testHttpPort)
            clients.add(Protocol.FILE)
            defaultHost.attach(new ChangedFeedApp())
            start()
        }
    }

    @AfterClass
    static void tearDownClass() {
        component.stop()
    }

    @Test
    void shouldLoadRdf() {
        def conn = repo.connection

        def url = { new URL(it) }
        def lit = repo.valueFactory.&createLiteral

        def thing = {
            repo.valueFactory.createURI("http://example.org/things/$it")
        }
        def countContexts = {
            def res = conn.contextIDs
            def i = res.asList().size()
            res.close()
            return i
        }

        assertEquals 0, countContexts()

        loader.readFeed(url("${baseUrl}/1-init.atom"))
        assertEquals 2, countContexts()
        assertTrue conn.hasStatement(thing(1), RDFS.LABEL, lit("Thing 1"), false)
        assertTrue conn.hasStatement(thing(2), RDFS.LABEL, lit("Thing 2"), false)        

        // No changes
        loader.readFeed(url("${baseUrl}/1-init.atom"))
        assertEquals 2, countContexts()
        assertTrue conn.hasStatement(thing(1), RDFS.LABEL, lit("Thing 1"), false)

        loader.readFeed(url("${baseUrl}/2-updated_t1.atom"))
        assertEquals 2, countContexts()
        assertTrue conn.hasStatement(thing(1), RDFS.LABEL, 
        		lit("Updated thing 1"), false)

        loader.readFeed(url("${baseUrl}/3-added_t3.atom"))
        assertEquals 3, countContexts()
        assertTrue conn.hasStatement(thing(3), RDFS.LABEL, lit("Thing 3"), false)

        loader.readFeed(url("${baseUrl}/4-deleted_t3.atom"))
        assertEquals 3, countContexts() // NOTE: keeps context..
        // TODO: assertEquals contextTimeStamp..
        assertFalse conn.hasStatement(thing(3), RDFS.LABEL, null, false)

        loader.readFeed(url("${baseUrl}/5-deleted_t2.atom"))
        assertEquals 3, countContexts() // NOTE: keeps context..        
        assertFalse conn.hasStatement(thing(2), RDFS.LABEL, null, false)

        loader.readFeed(url("${baseUrl}/6-deleted_t1.atom"))
        assertEquals 3, countContexts() // NOTE: keeps context..        
        assertFalse conn.hasStatement(thing(1), RDFS.LABEL, null, false)

        //DEBUG:RDFUtil.serialize(repo, "application/rdf+xml", System.out)
        conn.close()
    }

}

class ChangedFeedApp extends Application {
    static ROOT_URI = new File(
            "src/test/resources/feeds").toURI().toString()
    Restlet createRoot() {
        return new Directory(context, ROOT_URI)
    }
}
