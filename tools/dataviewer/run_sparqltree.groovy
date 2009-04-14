import org.openrdf.repository.http.HTTPRepository
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree
import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.BasicViewHandler


if (args.length < 1 || args.length == 2) {
    println "Usage: %prog <QUERY_PATH> [TEMPLATE_PATH LOCALE]"
    System.exit 1
}

// TODO: use rinfo-rdf-repo + config as in service!
def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo")

def queryPath = args[0]

if (args.length == 3) {
    def viewPath = args[1]
    def locale = args[2]
    def viewer = new SparqlTreeViewer(repo, queryPath, viewPath)
    println viewer.execute(
            new BasicViewHandler(locale, [encoding: "utf-8"]))

} else {
    def query = new File(queryPath).text
    def time = new Date()
    def tree = SparqlTree.runQuery(repo, query)
    def diff = (new Date().time - time.time) / 1000.0
    println "SparqlTree done in ${diff} s."
    tree.each { println it }
}
