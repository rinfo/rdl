@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
import static org.openrdf.query.QueryLanguage.SPARQL
import org.openrdf.repository.http.HTTPRepository
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree
@Grab('org.codehaus.jackson:jackson-mapper-asl:1.8.1')
import org.codehaus.jackson.map.ObjectMapper
import static org.codehaus.jackson.map.SerializationConfig.Feature.INDENT_OUTPUT

class ElasticIndexer {

    def repo
    def sparqlTree = new SparqlTree()

    ElasticIndexer(repo) {
        this.repo = repo
    }

    void indexTripleStore(int limit=-1) {
        int i = 0
        def conn = repo.connection
        try {
            def res = findPrimaryUris(conn, limit)
            while(res.hasNext()) {
                if (limit && i++ == limit) break
                def uri = res.next().getValue('topic')
                topicToElastic(uri)
            }
        } finally {
            conn.close()
        }
    }

    def findPrimaryUris(conn, limit) {
        def limitClause = limit == -1? "" : "limit ${limit}"
        def pq = conn.prepareTupleQuery(SPARQL, """
            prefix foaf: <http://xmlns.com/foaf/0.1/>
            select ?topic { graph ?g {
                ?g foaf:primaryTopic ?topic
            } } ${limitClause}""")
        return pq.evaluate()
    }

    void topicToElastic(uri) {
        def tree = toElastic(topicToTree(uri))
        println "Elastic Tree for: ${uri}"
        def jsonMapper = new ObjectMapper()
        jsonMapper.configure(INDENT_OUTPUT, true)
        println jsonMapper.writeValueAsString(tree)
        //TODO: elasticDb.index tree
    }

    def topicToTree(uri) {
        // TODO: gather data for desired indexing
        def tree = sparqlTree.runQuery(repo, """
        prefix dct: <http://purl.org/dc/terms/>
        prefix foaf: <http://xmlns.com/foaf/0.1/>

        select * {

            filter(?topic = <$uri>)

            ?topic a ?topic__type;
                dct:identifier ?topic__identifier;
                dct:publisher ?topic__1_publisher .

            ?topic__1_publisher foaf:name ?topic__1_publisher__1_name

        } """)
    }

    def toElastic(tree) {
        // TODO: convert to suitable record
        tree.topic[0]
    }

}

def repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", "rinfo")
def elIndex = new ElasticIndexer(repo)
elIndex.indexTripleStore(100)
