import net.sf.json.groovy.JsonSlurper

import org.antlr.stringtemplate.StringTemplateGroup

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens
import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder

import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.ModelViewHandler


def repo = RDFUtil.slurpRdf(
        "../../resources/base/model",
        "../../resources/base/extended/rdf",
        "../../resources/external/rdf")
//def repo = getRepo("http://localhost:8080/openrdf-sesame", "rinfo")

def templates = new StringTemplateGroup("sparqltrees",
        "../../resources/sparqltrees")
def rqViewer = new SparqlTreeViewer(repo, templates,
        "model/model-tree-rq", "model/model_html")

def labelTree = new JsonSlurper().parse(new File(
                "../../resources/sparqltrees/model/model_labels.json"))

def locale = 'sv'
println rqViewer.execute(new ModelViewHandler(locale, null, labelTree))

