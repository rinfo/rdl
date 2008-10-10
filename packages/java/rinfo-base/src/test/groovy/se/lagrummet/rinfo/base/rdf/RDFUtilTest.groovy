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

import org.junit.Test
import static org.junit.Assert.*


class RDFUtilTest {

    @Test
    void shouldCreateDateTime() {
        def vf = new ValueFactoryImpl()
        def time = new Date(0)
        def dtLiteral = RDFUtil.createDateTime(vf, time)
        assertEquals '"1970-01-01T00:00:00.000Z"' +
                '^^http://www.w3.org/2001/XMLSchema#dateTime',
                dtLiteral.toString()
    }

    @Test
    void shouldReplaceURI() {
        def repo = RDFUtil.createMemoryRepository()
        def vf = repo.valueFactory

        def oldURI = vf.createURI("http://example.com/stuff/item/1")
        def newURI = vf.createURI("http://example.org/things/item/one")
        def oldSubURI = vf.createURI("${oldURI}#fragment")
        def newSubURI = vf.createURI("${newURI}#fragment")

        def repoConn = repo.connection

        repoConn.add(oldURI, RDF.TYPE, RDFS.RESOURCE)
        repoConn.add(oldURI, RDFS.SEEALSO, oldSubURI)

        def newRepo = RDFUtil.replaceURI(repo, oldURI, newURI)
        def newRepoConn = newRepo.connection

        assertTrue newRepoConn.hasStatement(newURI, RDF.TYPE, RDFS.RESOURCE, false)
        assertTrue newRepoConn.hasStatement(newURI, RDFS.SEEALSO, newSubURI, false)
    }

}
