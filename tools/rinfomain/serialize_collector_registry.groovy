
@Grapes([
    @Grab('org.openrdf.sesame:sesame-repository-api:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-repository-sail:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-sail-memory:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-queryparser-sparql:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-api:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-rdfxml:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-turtle:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-n3:2.3.0'),
    //@Grab('org.slf4j:slf4j-api:1.5.0'),
    //@Grab('org.slf4j:slf4j-jcl:1.5.0'),
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
])
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.base.rdf.RDFUtil


if (args.length < 1) {
    println "Usage: <sesame-repo>"
    System.exit 1
}
def dataDir = new File(args[0])

def repo = new SailRepository(new NativeStore(dataDir))
repo.initialize()

conn = repo.connection
//conn.setNamespace("rconn", COLLECTOR_NS)
conn.close()

def mtype = "application/x-turtle"
//def mtype = "application/rdf+xml"
RDFUtil.serialize(repo, mtype, System.out)

repo.shutDown()

