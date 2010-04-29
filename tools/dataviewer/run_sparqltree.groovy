@Grapes([
    @Grab('net.sf.json-lib:json-lib:2.2.3:jdk15'),
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
])
import org.openrdf.repository.http.HTTPRepository
import net.sf.json.JSONSerializer

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree


if (args.length < 1 || args.length == 3) {
    println "Usage: <QUERY_PATH> <REPO_SOURCE..>"
    System.exit 1
}


def queryPath = args[0]
def rdfSource = args[1..args.length-1]

def repo = (rdfSource[0] == "rinfo")?
    new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo") :
    RDFUtil.slurpRdf(rdfSource as String[])


try {
    def query = new File(queryPath).text
    println "// Running.."
    def time = new Date()
    def tree = new SparqlTree().runQuery(repo, query)
    def diff = (new Date().time - time.time) / 1000.0
    println "// SparqlTree done in ${diff} s."
    println JSONSerializer.toJSON(tree).toString(4)
} finally {
    repo.shutDown()
}

