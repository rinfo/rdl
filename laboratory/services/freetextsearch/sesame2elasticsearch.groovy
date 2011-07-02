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
    def indexName = "rinfo"

    def sparqlTree = new SparqlTree(
            uriKey: "URI",
            bnodeKey: 'BNODE',
            datatypeKey: 'DATATYPE',
            valueKey: 'VALUE',
            langTag: "_",
            keepNull: false)

    ElasticIndexer(repo, esClient) {
        this.repo = repo
        this.esClient = esClient
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
                def elasticObj = topicToElastic(uri, type)
                println "[${i}] For <${uri}> of type <${type}>..."
                if (elasticObj) {
                    indexElastic elasticObj
                } else {
                    println "Empty; skipped."
                }
            }
            println "Done. Indexed ${i} objects."
        } finally {
            conn.close()
        }
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
            println "Indexing ${indexType}/<${id}>..."
            IndexRequestBuilder irb = esClient.prepareIndex(indexName, indexType, id).
                setConsistencyLevel(WriteConsistencyLevel.DEFAULT).
                setSource(data)
            irb.execute().actionGet()
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

    def topicToElastic(uri, type) {
        // TODO: switch on type for *ToTree(uri)
        return createData(uri, 'get_doc_tree.rq', 'doc')
    }

    def createData(uri, queryRef, key) {
        // TODO: gather data for desired indexing
        def ins = this.class.getResourceAsStream(queryRef)
        try {
            def query = ins.getText('utf-8').replaceAll('URI', "${uri}")
            def tree = sparqlTree.runQuery(repo, query)
            // TODO: massage to suitable record
            def obj = tree[key][0]
            return (obj)? [(key): obj] : null
        } finally {
            ins.close()
        }
    }

}


def repoName = args[0]
def limit = args.length > 1? args[1] as int : 100

def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", repoName)

def client = new TransportClient()
    .addTransportAddress(new InetSocketTransportAddress("127.0.0.1", 9300))

try {
    def elIndex = new ElasticIndexer(repo, client)
    elIndex.indexTripleStore(limit)
} finally {
    client.close()
}

