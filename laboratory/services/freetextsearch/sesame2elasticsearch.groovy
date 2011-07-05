@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
import static org.openrdf.query.QueryLanguage.SPARQL
import org.openrdf.repository.Repository
import org.openrdf.repository.http.HTTPRepository
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree

@Grab('org.codehaus.jackson:jackson-mapper-asl:1.8.1')
import org.codehaus.jackson.map.ObjectMapper
import static org.codehaus.jackson.map.SerializationConfig.Feature.INDENT_OUTPUT

@GrabResolver (name='Sonatype', root='https://oss.sonatype.org/content/repositories/releases')
@Grab('org.elasticsearch:elasticsearch:0.16.2')
import org.elasticsearch.action.WriteConsistencyLevel
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.client.Client
import org.elasticsearch.client.action.index.IndexRequestBuilder
import org.elasticsearch.client.action.search.*
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory
//import org.elasticsearch.index.query.xcontent.*//QueryBuilders


class ElasticIndexer {

    Repository repo
    Client esClient
    String indexName
    def elastifier
    boolean dryRun

    ElasticIndexer(repo, esClient, indexName, dryRun=false) {
        this.repo = repo
        this.esClient = esClient
        this.dryRun = dryRun
        this.elastifier = new Elastifier(repo)
    }

    void indexTripleStore(int limit=-1) {
        int i = 0
        def conn = repo.connection
        try {
            def res = findPrimaryUris(conn, limit)
            while(res.hasNext()) {
                if (limit > -1 && i == limit) {
                    break
                }
                i++
                def row = res.next()
                def uri = row.getValue('topic')
                def type = row.getValue('type')
                def elasticObj = elastifier.toElastic(uri, type)
                println "[${i}] For <${uri}> of type <${type}>..."
                if (elasticObj == null) {
                    println "Empty; skipped."
                    continue
                }
                indexElastic elasticObj
            }
            println "Done. Indexed ${dryRun? 0 : i} objects."
        } finally {
            conn.close()
        }
    }

    def findPrimaryUris(conn, limit) {
        def limitClause = limit == -1? "" : "limit ${limit}"
        def pq = conn.prepareTupleQuery(SPARQL, """
            prefix foaf: <http://xmlns.com/foaf/0.1/>
            select ?topic ?type { graph ?g {
                ?g foaf:primaryTopic ?topic .
                ?topic a ?type .
            } } ${limitClause}""")
        return pq.evaluate()
    }

    void indexElastic(obj) {
        def jsonMapper = new ObjectMapper()
        jsonMapper.configure(INDENT_OUTPUT, true)
        println "Elastic Object:"
        println jsonMapper.writeValueAsString(obj)

        obj.entrySet().each {
            def data = it.value
            def indexType = it.key
            def id = data.URI
            if (dryRun) {
                println "Dry run; no indexing done."
                return
            }
            // TODO:
            //def contentRef = findContentRef(id)
            //data['content'] = [
            //    '_content_type': contentRef.mediaType,
            //    'content': contentRef.base64EncodedValue
            //]
            println "Indexing ${indexType}/<${id}>..."
            IndexRequestBuilder irb = esClient.prepareIndex(indexName, indexType, id).
                setConsistencyLevel(WriteConsistencyLevel.DEFAULT).
                setSource(data)
            irb.execute().actionGet()
        }
    }

}


class Elastifier {

    Repository repo

    Elastifier(repo) {
        this.repo = repo
    }
    def sparqlTree = new SparqlTree() {
        {
            uriKey = "URI"
            bnodeKey = 'BNODE'
            datatypeKey = 'DATATYPE'
            valueKey = 'VALUE'
            langTag = "LANG_"
            keepNull = false
            keepEmpty = false
        }

        @Override
        Object completeNode(Object node, String key, Map parentNode) {
            if (isResource(node)) {
                // TODO:
                // Terms are proably the most useful (for facets etc.),
                // and path for docs.
                // But perhaps check if uri within expected uri space,
                // and keep URI in a field which isn't indexed? (Generally
                // good, and here to support simple roundtrip back to RDF data
                // about referenced resource.)
                if (parentNode.containsKey(uriKey)) { // node is not at top
                    node['term'] = getUriTerm(node.remove(uriKey))
                } else {
                    node[uriKey] = new URI(node[uriKey]).path
                }
                return node

            } else if (isDatatypeNode(node)) {
                // TODO: somehow note if datatype is date?
                return node[valueKey]

            } else if (isLangNode(node)) {
                def keys = node.keySet()
                if (keys.size() == 1) {
                    def langKey = keys.toArray()[0]
                    //parentNode.get('lang', []) << langKey.substring(langTag.size())
                    return node[langKey]
                }
                for (langKey in keys) {
                    node[langKey.substring(langTag.size())] = node.remove(langKey)
                }
                return node

            } else {
                return node
            }
        }

        def getUriTerm(uri) {
            uri.substring(
                    uri.lastIndexOf(uri.contains('#')? '#' : '/') + 1,
                    uri.size())
        }

    }

    def toElastic(uri, type) {
        // TODO: switch on type for *ToTree(uri)
        return createData(uri, 'get_doc_tree.rq', 'doc')
    }

    def createData(uri, queryRef, key) {
        def ins = this.class.getResourceAsStream(queryRef)
        try {
            def query = ins.getText('utf-8').replaceAll('URI', "${uri}")
            def tree = sparqlTree.runQuery(repo, query)
            def obj = tree[key]?.getAt(0)
            return (obj)? [(key): obj] : null
        } finally {
            ins.close()
        }
    }

}


def repoIndexName = args[0]
int limit = args.length > 1? args[1] as int : 100
boolean dryRun = args.length > 2? args[2] == 'dry' : false

def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", repoIndexName)

def client = new TransportClient()
    .addTransportAddress(new InetSocketTransportAddress("127.0.0.1", 9300))

try {
    def elIndex = new ElasticIndexer(repo, client, repoIndexName, dryRun)
    elIndex.indexTripleStore(limit)
} finally {
    client.close()
}

