@Grab(group='se.lagrummet.rinfo', module='rinfo-base', version='1.0-SNAPSHOT')
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

