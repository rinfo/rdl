import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.nativerdf.NativeStore
import org.openrdf.sail.rdbms.mysql.MySqlStore
import se.lagrummet.rinfo.base.rdf.RDFUtil
import groovy.text.SimpleTemplateEngine


if (args.length < 1) {
    println "Usage: <path-to-sesame-repo> [number-of-items]"
    System.exit 1
}

def numberOfItems = (args.length > 1) ? new Integer(args[1]) : 1000

def storeref = args[0]
def repo
switch (storeref) {
    case "mem":
        repo = RDFUtil.createMemoryRepository()
        break
    case "mysql":
        def store = new MySqlStore("sesame_test")
        store.user = "root"
        repo = new SailRepository(store)
        repo.initialize()
        break
    case ~/\/.+/:
        repo = new SailRepository(new NativeStore(new File(storeref), "spoc,posc,ospc,opsc"))
        repo.initialize()
        break
}

//System.exit(0)
repo.connection.clear()


/**
 * Create test repo with data
 */

def engine = new SimpleTemplateEngine()
def tplt = engine.createTemplate('''
    @prefix awol: <http://bblfish.net/work/atom-owl/2006-06-06/#> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

    <${doc}#entry> a awol:Entry;
        awol:id <${doc}>;
        awol:title "${title}"@en;
        awol:updated "${updated}"^^xsd:dateTime .
    <% rels.each { %>
        <${doc}> <${it.rel}> <${it.relDoc}> .
    <% } %>
''')

def load(repo, n3str) {
    def is = new ByteArrayInputStream(n3str.getBytes("utf-8"))
    RDFUtil.loadDataFromStream(repo, is, "", "application/x-turtle")
}


def docUris = []

def randomRel(uris, limit=8) {
    def rand = new Random()
    def result = []
    def max = rand.nextInt(limit)
    for (uri in uris) {
        if (result.size() == max)
            break
        if (rand.nextBoolean()) {
            def randRel = "http://example.org/ns/stuff#rel-${rand.nextInt(10)}"
            result << [rel:randRel, relDoc:uri]
        }
    }
    return result
}

println "Adding ${numberOfItems} items."
numberOfItems.times {
    if (it % 100 == 0) print "${it} items.. "

    def date = new Date(
            ( (new Random().nextInt(Math.ceil(new Date().time / 1000) as int))
                * 1000.0 ).toLong()
        )
    def dateStr = RDFUtil.createDateTime(
            repo.valueFactory, date).calendarValue().toString()

    def item = [
        doc: "http://example.org/${dateStr}/item-${it}",
        title: "Item ${it}",
        updated: dateStr,
        rels: randomRel(docUris)
    ]

    docUris << item.doc // TODO: on large numberOfItems, how slow does this get?
    load(repo, tplt.make(item).toString())
}
println("Done.")

repo.shutDown()

