import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import static org.apache.commons.io.FileUtils.iterateFiles

import se.lagrummet.rinfo.base.rdf.SparqlTree
import se.lagrummet.rinfo.base.rdf.RDFUtil

def loadedRepo() {
    def repo = RDFUtil.createMemoryRepository()
    ["base/model", "base/extended/rdf", "external/rdf", "base/datasets"].each {
        iterateFiles(new File("../../../resources/", it),
                ["n3", "rdf", "rdfs", "owl"] as String[], true).each {
            println "Loading: ${it}"
            RDFUtil.loadDataFromFile(repo, it)
        }
    }
    return repo
}

def runSparqlTree(repo, tree, formatter) {
    def treeDir = new File("../../../resources/sparqltrees/")
    def rqTree = new SparqlTree(repo, new File(treeDir, tree))
    def time = new Date()

    /*
    groovy.xml.dom.DOMUtil.serialize(
            rqTree.queryToDocument().documentElement, System.out)
    */

    println "Using SPARQL: " + rqTree.queryString

    def out = new StreamResult(System.out)
    if (formatter) {
        def outputXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
                new StreamSource(new File(treeDir, formatter)))
        rqTree.queryAndChainToResult(out, outputXslt)
    } else {
        rqTree.queryAndChainToResult(out)
    }

    def diff = (new Date().time - time.time) / 1000.0
    println "Done in ${diff} s."
}

def tree = args[0]
def formatter = args.length==2? args[1] : null
runSparqlTree loadedRepo(), tree, formatter

