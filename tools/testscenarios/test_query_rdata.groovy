// NOTE:
//  $ export JAVA_OPTS="-Xms64m -Xmx512m"

import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore
//import org.openrdf.sail.rdbms.mysql.MySqlStore
import org.openrdf.query.QueryLanguage
import se.lagrummet.rinfo.base.rdf.RDFUtil
import groovy.text.SimpleTemplateEngine



if (args.length < 1) {
    println "Usage: <path-to-sesame-repo> [limit] [offset]"
    System.exit 1
}
def storeref = args[0]
def repo
switch (storeref) {
    case "mem":
        repo = RDFUtil.createMemoryRepository()
        break
    /*
    case "mysql":
        def store = new MySqlStore("sesame_test")
        store.user = "root"
        repo = new SailRepository(store)
        repo.initialize()
        break
    */
    case ~/\/.+/:
        repo = new SailRepository(new NativeStore(new File(storeref), "spoc,posc,ospc,opsc"))
        repo.initialize()
        break
}


if ("-d" in args) {
    RDFUtil.serialize(repo, "text/rdf+n3", System.out)
    System.exit(0)
}

def limit = (args.length > 1) ? new Integer(args[1]) : 100
def offset = (args.length > 2) ? new Integer(args[2]) : 0

/**
 * Query test repo and measure time.
 */

def filters
filters = """
    FILTER(?updated > "2007-12-31T00:00:00Z"^^xsd:dateTime)
"""
//filters = ""

// TODO: "felanvänd" awol:id.. foaf:primaryTopic? eller .. vad?
def queryStr = """
    PREFIX awol: <http://bblfish.net/work/atom-owl/2006-06-06/#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

    # NOTE: DISTINCT somewhat reduces performance (~1 s).

    SELECT * WHERE {

        ?entry a awol:Entry;
            awol:id ?doc;
            awol:title ?title;
            awol:updated ?updated .

        ${filters}

        # FIXME: performance is *very* reduced if we include
        # rels and revs. Should to a two-pass to retriveve
        # these (only revs? rels would be in persisted entry),
        # after this query has found the relevant docs
        # based on text and and category filtering.

        #OPTIONAL {
        #    ?doc ?rel ?relDoc .
        #    ?relEntry awol:id ?relDoc .
        #}
        #OPTIONAL {
        #    ?revDoc ?rev ?doc .
        #    ?revEntry awol:id ?revDoc .
        #}

    }
    ORDER BY DESC(?updated)
    LIMIT ${limit} OFFSET ${offset}
"""

def conn = repo.getConnection()
def tupleQuery = conn.prepareTupleQuery(
        QueryLanguage.SPARQL, queryStr)
tupleQuery.includeInferred = false

def start = new Date()
println "Querying.."
def result = tupleQuery.evaluate()

def prevDoc = null
while (result.hasNext()) {
    def map = [:]
    def row = result.next()
    result.bindingNames.each {
        map[it] = row.getValue(it)
    }
    println "${map.title}, ${map.updated}, ${map.doc}"
    if (map.doc != prevDoc) {
        println()
    }
    prevDoc = map.doc
}

def ms = new Date().time - start.time
println "Query complete (in ${ms/1000} s)."
result.close()
conn.close()
repo.shutDown()


