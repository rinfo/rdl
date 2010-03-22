@Grapes([
    @Grab('org.antlr:stringtemplate:3.2.1'),
    @Grab('net.sf.json-lib:json-lib:2.2.3:jdk15'),
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
])
import org.openrdf.repository.http.HTTPRepository
import org.antlr.stringtemplate.StringTemplateGroup
import net.sf.json.JSONSerializer
import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.RDataViewHandler


def (flags, posArgs) = args.split { it =~ /^--/ }
def queryPath = posArgs[0]
def viewPath = posArgs[1]
def path = posArgs[2]
def locale = "sv"

def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo")

def templates = new StringTemplateGroup("sparqltrees")
def viewer = new SparqlTreeViewer(repo, templates, queryPath, viewPath)
def appData = [:]

def rinfoUri = "http://rinfo.lagrummet.se/publ/${path}"
def filter = "FILTER(?doc = <${rinfoUri}>)"
def queryData = [
        filter:filter,
        get_relrev: true
    ]

def rdataVH = new RDataViewHandler(locale, appData, queryData)
if ('-d' in flags) {
    def tree = viewer.execQuery(rdataVH)
    tree.remove('query')
    println JSONSerializer.toJSON(tree).toString(4)
} else {
    println viewer.execute(rdataVH)
}

