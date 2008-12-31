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


def treeDir = new File("../../../laboratory/services/SparqlToAtom/examples/")

def rqTree = new SparqlTree(repo, new File(treeDir, "rdata/rpubl-rqtree.xml"))

//def doc = rqTree.queryToTreeDocument()
//SparqlTree.TRANSFORMER_FACTORY.newTransformer().transform(
//    new DOMSource(doc), new StreamResult(System.out))
def outputXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
        new StreamSource(new File(treeDir, "rdata/rpubl_to_rdata.xslt")))
rqTree.queryAndChainToResult(new StreamResult(System.out), outputXslt)

