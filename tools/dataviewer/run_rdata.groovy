@Grapes([
    @Grab('org.antlr:stringtemplate:3.2.1'),
    @Grab('net.sf.json-lib:json-lib:2.2.3:jdk15'),
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
    @Grab('org.restlet:org.restlet:1.1.4')
])
import org.openrdf.repository.http.HTTPRepository
import org.antlr.stringtemplate.StringTemplate
import org.antlr.stringtemplate.StringTemplateGroup
import net.sf.json.JSONSerializer
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree

import static se.lagrummet.rinfo.service.SparqlTreeRouter.APP_DATA
import se.lagrummet.rinfo.service.dataview.TemplateUtil
import se.lagrummet.rinfo.service.dataview.RDataSparqlTree


def (flags, posArgs) = args.split { it =~ /^-/ }

def queryPath = posArgs[0]
def viewPath = posArgs[1]
def path = posArgs[2]
def locale = "sv"

def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo")

def appData = APP_DATA
def queryData = [
    "max_limit": 4096,
]
if (path) {
    queryData["path"] = path
    queryData["details"] = true
} else {
    // for list
    //queryData["filter_parts"] = [
    //    ["typeSelector": "rpubl:Lag"],
    //    ["publisherSelector":  true, "value": "regeringskansliet"],
    //    ["dateSelector": "rpubl:utfardandedatum", "value": "1918"],
    //]
    queryData["filter_parts"] = [
        ["typeSelector": "rpubl:Forordning"],
        ["publisherSelector":  true, "value": "regeringskansliet"],
        ["dateSelector": "rpubl:utfardandedatum", "value": "2007"],
    ]
    // for browse
    queryData["docType"] = "rpubl:Rattsfallsreferat"
    queryData["publisher"] = "domstolsverket"
}

def templates = new StringTemplateGroup("sparqltrees")
def tpltUtil = new TemplateUtil(templates)
def dataRqTree = new RDataSparqlTree(appData, locale)
def query = tpltUtil.runTemplate(queryPath, queryData)

println "=" * 72

if ('-q' in flags) {
    println()
    println query
    println()
} else {
    def start = new Date()
    println "Running query..."
    def tree = null
    try {
        tree = dataRqTree.runQuery(repo, query)
    } finally {
        repo.shutDown()
    }
    println "=" * 72
    println()
    if ('-d' in flags) {
        println JSONSerializer.toJSON(tree).toString(4)
    } else {
        println tpltUtil.runTemplate(viewPath, tree)
    }
    println()
    println "=" * 72
    def duration = new Date().time - start.time
    println "Done in ${duration/1000} s."
}

println "=" * 72

