import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import static org.apache.commons.io.FileUtils.iterateFiles

import se.lagrummet.rinfo.base.rdf.SparqlTree
import se.lagrummet.rinfo.base.rdf.RDFUtil

def loadedRepo() {
    def repo = RDFUtil.createMemoryRepository()
    ["model", "extended/rdf", "datasets"].each {
        iterateFiles(new File("../../../resources/base/", it),
                ["n3"] as String[], true).each {
            println "Loading: ${it}"
            RDFUtil.loadDataFromFile(repo, it)
        }
    }
    return repo
}

def runSparqlTree(repo, tree, formatter) {
    def treeDir = new File("../../../laboratory/services/SparqlToAtom/examples/")
    def rqTree = new SparqlTree(repo, new File(treeDir, tree))
    println "Using SPARQL: " + rqTree.queryString

    def outputXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
            new StreamSource(new File(treeDir, formatter)))
    rqTree.queryAndChainToResult(new StreamResult(System.out), outputXslt)
}

def argl = args as List
def tree = argl[0] ?: "model/sparqltree-model.xml"
def formatter = argl[1] ?: "model/modeltree_to_html.xslt"
runSparqlTree loadedRepo(), tree, formatter

