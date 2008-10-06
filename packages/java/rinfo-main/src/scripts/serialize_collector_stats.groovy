
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.base.rdf.RDFUtil

if (args.length < 1) {
    println "Usage: <sesame-repo>"
    System.exit 1
}
def dataDir = new File(args[0])

def statsRepo = new SailRepository(new NativeStore(dataDir))
statsRepo.initialize()

def mtype = "application/x-turtle"
//def mtype = "application/rdf+xml"
RDFUtil.serialize(statsRepo, mtype, System.out)

statsRepo.shutDown()

