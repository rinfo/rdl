package se.lagrummet.rinfo.base.rdf

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*

import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import se.lagrummet.rinfo.base.rdf.RDFUtil


class SparqlTreeTest {

    SparqlTree rqTree
    File treeDir

    @Before
    void setup() {
        def repo = RDFUtil.createMemoryRepository()
        // FIXME: move these to library/base..
        treeDir = new File("../../../laboratory/services/SparqlToAtom/")
        rqTree = new SparqlTree(repo, new File(treeDir, "sparqltree-model.xml"))
    }

    @Test
    void shouldQueryToTreeDocument() {
        def doc = rqTree.queryToTreeDocument()
    }

    @Test
    void shouldQueryAndChainToResult() {
        def toHtmlXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
                new StreamSource(new File(treeDir, "modeltree_to_html.xslt")))
        def outStream = new ByteArrayOutputStream()
        rqTree.queryAndChainToResult(new StreamResult(outStream), toHtmlXslt)
    }

}
