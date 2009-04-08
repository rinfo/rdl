import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens


def repo = RDFUtil.slurpRdf(
        "../../resources/base/model",
        "../../resources/base/extended/rdf",
        "../../resources/external/rdf")
//def repo = getRepo("http://localhost:8080/openrdf-sesame", "rinfo")

def query = new File(
        "../../resources/sparqltrees/model/model-tree.rq").text
def locale = 'sv'
println new ModelViewer(repo, query, new SmartLens(locale),
        "../../resources/sparqltrees/model/model_html.st",
        "../../resources/sparqltrees/model/model_labels.json").execute()

