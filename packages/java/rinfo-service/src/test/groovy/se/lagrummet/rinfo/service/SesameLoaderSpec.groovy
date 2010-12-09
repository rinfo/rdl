package se.lagrummet.rinfo.service

import org.restlet.*
import org.restlet.data.Protocol
import org.restlet.resource.Directory

import org.openrdf.model.vocabulary.RDFS
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore

import se.lagrummet.rinfo.base.rdf.RDFUtil

import spock.lang.*


class SesameLoaderSpec extends Specification {

    @Shared repo
    @Shared component
    @Shared httpPort = 9991
    def baseUrl = "http://localhost:${httpPort}"

    def setupSpec() {
        repo = RDFUtil.createMemoryRepository()
        component = new Component()
        component.servers.add(Protocol.HTTP, httpPort)
        component.clients.add(Protocol.FILE)
        component.defaultHost.attach(new ChangedFeedApp())
        component.start()
    }

    def cleanupSpec() {
        component.stop()
    }

    def loadFeed(String url) {
        def loader = new SesameLoader(repo)
        try {
            loader.readFeed(new URL(url))
        } finally {
            loader.shutdown()
        }

    }

    def "should load rdf"() {
        setup:
        def conn = repo.getConnection()
        and:
        def lit = repo.valueFactory.&createLiteral
        def thing = {
            repo.valueFactory.createURI("http://example.org/things/$it")
        }
        def countContexts = { RepoInfo.countContexts(conn) }
        and:
        countContexts() == 0

        when:
        loadFeed("${baseUrl}/1-init.atom")
        then:
        countContexts() == 2
        conn.hasStatement(thing(1), RDFS.LABEL, lit("Thing 1"), false)
        conn.hasStatement(thing(2), RDFS.LABEL, lit("Thing 2"), false)

        // No changes
        when:
        loadFeed("${baseUrl}/1-init.atom")
        then:
        countContexts() == 2
        conn.hasStatement(thing(1), RDFS.LABEL, lit("Thing 1"), false)

        when:
        loadFeed("${baseUrl}/2-updated_t1.atom")
        then:
        countContexts() == 2
        conn.hasStatement(thing(1), RDFS.LABEL, 
                        lit("Updated thing 1"), false)

        when:
        loadFeed("${baseUrl}/3-added_t3.atom")
        then:
        countContexts() == 3
        conn.hasStatement(thing(3), RDFS.LABEL, lit("Thing 3"), false)

        when:
        loadFeed("${baseUrl}/4-deleted_t3.atom")
        then:
        countContexts() == 2
        // TODO: assertEquals contextTimeStamp..
        ! conn.hasStatement(thing(3), RDFS.LABEL, null, false)

        when:
        loadFeed("${baseUrl}/5-deleted_t2.atom")
        then:
        countContexts() == 1
        ! conn.hasStatement(thing(2), RDFS.LABEL, null, false)

        when:
        loadFeed("${baseUrl}/6-deleted_t1.atom")
        then:
        countContexts() == 0
        ! conn.hasStatement(thing(1), RDFS.LABEL, null, false)

        cleanup:
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
