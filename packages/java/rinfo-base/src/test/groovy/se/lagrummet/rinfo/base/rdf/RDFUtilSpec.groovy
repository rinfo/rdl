package se.lagrummet.rinfo.base.rdf

import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.sail.SailRepository
import org.openrdf.rio.RDFFormat
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.model.Statement
import org.openrdf.model.URI
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.RDFS

import spock.lang.*


class RDFUtilSpec extends Specification {

    def "should create date time"() {
        when:
        def vf = new ValueFactoryImpl()
        def time = new Date(0)
        def dtLiteral = RDFUtil.createDateTime(vf, time)
        then:
        dtLiteral.toString() ==
                '"1970-01-01T00:00:00.000Z"' +
                '^^<http://www.w3.org/2001/XMLSchema#dateTime>'
    }

    def "should replace uri"() {
        given:
        def repo = RDFUtil.createMemoryRepository()
        def vf = repo.valueFactory
        and:
        def oldURI = vf.createURI("http://example.com/stuff/item/1")
        def newURI = vf.createURI("http://example.org/things/item/one")
        def oldSubURI = vf.createURI("${oldURI}#fragment")
        def newSubURI = vf.createURI("${newURI}#fragment")
        and:
        def repoConn = repo.connection
        and:
        repoConn.add(oldURI, RDF.TYPE, RDFS.RESOURCE)
        repoConn.add(oldURI, RDFS.SEEALSO, oldSubURI)

        when:
        def newRepo = RDFUtil.replaceURI(repo, oldURI, newURI)
        def newRepoConn = newRepo.connection

        then:
        assert newRepoConn.hasStatement(newURI, RDF.TYPE, RDFS.RESOURCE, false)
        assert newRepoConn.hasStatement(newURI, RDFS.SEEALSO, newSubURI, false)
    }

    def "should parse RDFa"() {
        given:
        def repo = RDFUtil.createMemoryRepository()
        def conn = repo.getConnection()
        def loc = "src/test/resources/rdf/rdfa.xhtml"
        def stream = new FileInputStream(loc)
        and:
        def dcTitle = conn.valueFactory.createURI("http://purl.org/dc/terms/title")
        def theTitle = conn.valueFactory.createLiteral("The Title")
        def otherTitle = conn.valueFactory.createLiteral("Other Title")
        when:
        RDFUtil.loadDataFromStream(conn, stream, loc, "application/xhtml+xml")
        then:
        RDFUtil.one(conn, null, dcTitle, theTitle)
        !RDFUtil.one(conn, null, dcTitle, otherTitle)
        cleanup:
        conn.close()
    }

}
