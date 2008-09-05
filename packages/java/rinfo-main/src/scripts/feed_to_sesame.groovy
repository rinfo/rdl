
import org.openrdf.repository.Repository
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.service.SesameLoader


Repository repo = null

if (args.length > 1) {
    def path = args[1]
    if (path =~ /^https?:/) {
        repo = new HTTPRepository(path, args[2])
    } else {
        def dataDir = new File(path)
        repo = new SailRepository(new NativeStore(dataDir))
    }
} else {
    repo = new SailRepository(new MemoryStore())
}

repo.initialize()

def loader = new SesameLoader(repo)
loader.readFeed new URL(args[0])

if (repo instanceof SailRepository && repo.sail instanceof MemoryStore) {
    //def mtype = "application/x-turtle"
    def mtype = "application/rdf+xml"
    RDFUtil.serialize(repo, mtype, System.out)
}

repo.shutDown()

