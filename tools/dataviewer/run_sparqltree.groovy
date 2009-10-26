@Grab(group='se.lagrummet.rinfo', module='rinfo-service', version='1.0-SNAPSHOT')
def _(){}

import org.openrdf.repository.http.HTTPRepository
import net.sf.json.JSONSerializer

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree
import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.BasicViewHandler


if (args.length < 1 || args.length == 3) {
    println "Usage: <REPO_SOURCE> <QUERY_PATH> [TEMPLATE_PATH LOCALE]"
    System.exit 1
}


def rdfSource = args[0]
def queryPath = args[1]

def repo

if (rdfSource == "rinfo")
    // TODO: use config (as in service)
    repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo")
else
    repo = RDFUtil.slurpRdf(rdfSource)

if (args.length == 4) {
    def viewPath = args[2]
    def locale = args[3]
    def viewer = new SparqlTreeViewer(repo, queryPath, viewPath)
    println viewer.execute(
            new BasicViewHandler(locale, [encoding: "utf-8"]))

} else {
    def query = new File(queryPath).text
    def time = new Date()
    def tree = SparqlTree.runQuery(repo, query)
    def diff = (new Date().time - time.time) / 1000.0
    println "// SparqlTree done in ${diff} s."
    println JSONSerializer.toJSON(tree).toString(4)
}
