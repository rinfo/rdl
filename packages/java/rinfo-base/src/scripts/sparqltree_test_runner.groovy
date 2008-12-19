import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import se.lagrummet.rinfo.base.rdf.SparqlTree

//import se.lagrummet.rinfo.base.rdf.RDFUtil
//def repo = RDFUtil.createMemoryRepository()
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore
def repo = new SailRepository(new NativeStore(new File("/Users/nlm/Library/Application Support/Aduna/OpenRDF Sesame/repositories/rinfo/")))
repo.initialize()


def treeDir = new File("../../../laboratory/services/SparqlToAtom/")

def rqTree = new SparqlTree(repo, new File(treeDir, "sparqltree-model.xml"))

//def doc = rqTree.queryToTreeDocument()
//SparqlTree.TRANSFORMER_FACTORY.newTransformer().transform(
//    new DOMSource(doc), new StreamResult(System.out))
def toHtmlXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
        new StreamSource(new File(treeDir, "modeltree_to_html.xslt")))
rqTree.queryAndChainToResult(new StreamResult(System.out), toHtmlXslt)

