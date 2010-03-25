@Grapes([
    @Grab('org.antlr:stringtemplate:3.2.1'),
    @Grab('net.sf.json-lib:json-lib:2.2.3:jdk15'),
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
])
import org.openrdf.repository.http.HTTPRepository
import org.antlr.stringtemplate.StringTemplate
import org.antlr.stringtemplate.StringTemplateGroup
import net.sf.json.JSONSerializer
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree

import se.lagrummet.rinfo.service.dataview.TemplateUtil
import se.lagrummet.rinfo.service.dataview.RDataSparqlTree


def (flags, posArgs) = args.split { it =~ /^-/ }

def queryPath = posArgs[0]
def viewPath = posArgs[1]
def path = posArgs[2]
def locale = "sv"

def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo")

def appData = [
    "encoding": "utf-8",
    "resourceBaseUrl": "http://rinfo.lagrummet.se/",
    "basePath": "/rdata",
]
def queryData = [
    "max_limit": 1024,
]
if (path) {
    queryData["path"] = path
    queryData["details"] = true
} else {
    queryData["filter_parts"] = [
        ["typeSelector": "rpubl:Lag"],
        ["dateSelector": "rpubl:utfardandedatum", "value": "1918"],
        ["leafSelector":  "dct:publisher", "value": "regeringskansliet"],
    ]
}

def templates = new StringTemplateGroup("sparqltrees")
def tpltUtil = new TemplateUtil(templates)
def dataRqTree = new RDataSparqlTree(appData.resourceBaseUrl, locale)
def query = tpltUtil.runTemplate(queryPath, queryData)

println "=" * 72

if ('-q' in flags) {
    println()
    println query
    println()
} else {
    def start = new Date()
    println "Running query..."
    def tree = dataRqTree.runQuery(repo, query)
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

