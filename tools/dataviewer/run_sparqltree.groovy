import org.openrdf.repository.http.HTTPRepository
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree
import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer


if (args.length < 1 || args.length == 2) {
    println "Usage: %prog <QUERY_PATH> [TEMPLATE_PATH LOCALE]"
    System.exit 1
}

// TODO: use rinfo-rdf-repo + config as in service!
def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo")
def query = new File(args[0]).text

if (args.length == 3) {
    def tplt = args[1]
    def locale = args[2]
    def viewer = new SparqlTreeViewer(repo, query,
            new SmartLens(locale), tplt)
    println viewer.execute([encoding: "utf-8"])

} else {
    def time = new Date()
    def tree = SparqlTree.runQuery(repo, query)
    def diff = (new Date().time - time.time) / 1000.0
    println "SparqlTree done in ${diff} s."
    tree.each { println it }
}
