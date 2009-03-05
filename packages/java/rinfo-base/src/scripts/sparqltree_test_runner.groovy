import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import se.lagrummet.rinfo.base.rdf.SparqlTree

import se.lagrummet.rinfo.base.rdf.RDFUtil
import static org.apache.commons.io.FileUtils.iterateFiles


def repo = RDFUtil.createMemoryRepository()

["model", "extended/rdf"].each {
    iterateFiles(new File("../../../resources/base/", it),
            ["n3"] as String[], true).each {
        RDFUtil.loadDataFromFile(repo, it)
    }
}

def treeDir = new File("../../../laboratory/services/SparqlToAtom/examples/")
def rqTree = new SparqlTree(repo, new File(treeDir, "model/sparqltree-model.xml"))

def outputXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
        new StreamSource(new File(treeDir, "model/modeltree_to_html.xslt")))
rqTree.queryAndChainToResult(new StreamResult(System.out), outputXslt)

