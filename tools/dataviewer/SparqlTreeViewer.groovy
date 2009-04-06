import static org.apache.commons.io.FileUtils.iterateFiles
import org.openrdf.repository.Repository
import org.antlr.stringtemplate.StringTemplate

import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder
import se.lagrummet.rinfo.base.rdf.sparqltree.Lens
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree

import se.lagrummet.rinfo.base.rdf.RDFUtil


@Grab(group='org.antlr', module='stringtemplate', version='3.2')
class SparqlTreeViewer {

    static Repository loadRepo(basedir, datadirs) {
        def repo = RDFUtil.createMemoryRepository()
        datadirs.each {
            iterateFiles(new File(basedir, it),
                    ["n3", "rdf", "rdfs", "owl"] as String[], true).each {
                System.err.println "Loading: ${it}"
                RDFUtil.loadDataFromFile(repo, it)
            }
        }
        return repo
    }

    static Map queryToTree(Repository repo, String query) {
        return new SparqlTree(repo, query).runQuery()
    }

    static Map queryToGraph(Repository repo, String query, Lens lens) {
        def tree = queryToTree(repo, query)
        return GraphBuilder.buildGraph(lens, tree)
    }

    static StringTemplate preparedTemplate(String tpltPath, Map data) {
        def st = new StringTemplate(new File(tpltPath).text)
        data.each { key, value ->
            st.setAttribute(key, value)
        }
        return st
    }

    static void main(String[] args) {
        if (args.length < 1 || args.length == 2) {
            println "Usage: %prog <QUERY_PATH> [<TEMPLATE_PATH> <ROOT_KEY>]"
            System.exit 1
        }

        def repo = loadRepo("../../resources/",
                ["base/model", "base/datasets"]
            )

        def query = new File(args[0]).text

        if (args.length == 3) {
            def strTplt = args[1]
            def rootKey = args[2]
            def lens = new SmartLens('sv')
            def graph = queryToGraph(repo, query, lens)
            graph.org.each {
            }
            def st = preparedTemplate(strTplt, [
                    (rootKey): graph[rootKey]
                ])
            st.setAttribute("encoding", "utf-8")
            println st.toString()

        } else {
            def time = new Date()
            def tree = queryToTree(repo, query)
            def diff = (new Date().time - time.time) / 1000.0
            println "SparqlTree done in ${diff} s."
            tree.each { println it }
        }
    }

}
