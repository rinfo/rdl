import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.query.QueryLanguage
import org.openrdf.rio.RDFFormat
//import org.openrdf.sail.inferencer.fc.DirectTypeHierarchyInferencer
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer


@Grapes([
    @Grab('org.openrdf.sesame:sesame-repository-api:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-repository-sail:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-sail-memory:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-queryparser-sparql:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-api:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-rdfxml:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-turtle:2.3.0'),
    @Grab('org.openrdf.sesame:sesame-rio-n3:2.3.0'),
    @Grab('org.slf4j:slf4j-api:1.5.0'),
    @Grab('org.slf4j:slf4j-jcl:1.5.0')
    //@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
])
def runQuery(def conn, String query, boolean inferred) {
    def prepQuery = conn.prepareQuery(QueryLanguage.SPARQL, query)
    prepQuery.setIncludeInferred(inferred)
    return prepQuery.evaluate()
}

void loadRdf(conn, sources) {
    for (source in sources) {
        def file = new File(source)
        def format = RDFFormat.forFileName(file.getName())
        conn.add(new FileInputStream(file), file.toURI().toString(), format)
    }
}

def inferred = false
def repo = new SailRepository(inferred?
        new ForwardChainingRDFSInferencer(new MemoryStore()) :
        new MemoryStore())
repo.initialize()
def conn = repo.getConnection()
loadRdf(conn, args[1..args.length-1])
println "// Running.."
def time = new Date()
def res = runQuery(conn, new File(args[0]).text, inferred)
def diff = (new Date().time - time.time) / 1000.0
println "// Query done in ${diff} s."
println()
while(res.hasNext()) {
    def row = res.next()
    row.bindingNames.each {
        println "${it}: ${row.getValue(it)}"
    }
    println()
}
res.close()
conn.close()
repo.shutDown()

