package rinfo.service.cmd

import se.lagrummet.rinfo.service.ElasticData
import se.lagrummet.rinfo.service.ElasticLoader
import se.lagrummet.rinfo.service.JsonLdSettings
import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI
import org.openrdf.repository.http.HTTPRepository
import static org.openrdf.query.QueryLanguage.SPARQL


class GenElastic {

    static void main(args) {
        def indexName = args[0]
        def textDir = args.length > 1? args[1] as File : null
        new GenElastic(indexName, textDir).run()
    }

    def repo
    def elData
    def eLoader
    def textDir

    GenElastic(String indexName, File textDir=null) {
        repo = new HTTPRepository("http://localhost:8080/openrdf-sesame", indexName)
        elData = new ElasticData("127.0.0.1", 9300, indexName, new JsonLdSettings())
        eLoader = new ElasticLoader(elData) {
            @Override
            String getContentText(String url, collector) {
                return new File(new URI(url)).getText('UTF-8')
            }
        }
        this.textDir = textDir
    }

    void run() {
        println "Initializing ElasticData..."
        elData.initialize()
        try {
            indexTripleStore()
        } finally {
            elData.shutdown()
        }
    }

    void indexTripleStore(int limit=-1) {
        int i = 0
        def conn = repo.connection
        try {
            println "Retrieving primary IRIs..."
            def res = findPrimaryIris(conn, limit)
            println "Indexing..."
            while(res.hasNext()) {
                if (limit > -1 && i == limit) {
                    break
                }
                i++
                def row = res.next()
                def iri = row.getValue('topic').toString()
                println "${i}: <${iri}>"
                def entry = Abdera.instance.newEntry()
                entry.setId(iri)
                if (textDir) {
                    def data =  findTextDataFor(iri)
                    if (data) {
                        println "Using text from file: ${data.file} [${data.mediaType}]"
                        entry.setContent(new IRI(data.file.toURI()), data.mediaType)
                    }
                }
                eLoader.create(conn, entry, null)

            }
            println "Done. Indexed ${i} objects."
        } finally {
            conn.close()
        }
    }

    def findPrimaryIris(conn, limit) {
        def limitClause = limit == -1? "" : "limit ${limit}"
        def ptq = conn.prepareTupleQuery(SPARQL, """
            prefix foaf: <http://xmlns.com/foaf/0.1/>
            select ?topic ?type { graph ?g {
                ?g foaf:primaryTopic ?topic .
                ?topic a ?type .
            } } ${limitClause}""")
        return ptq.evaluate()
    }

    def findTextDataFor(String iri) {
        def dir = new File(textDir,
                new URI(iri).path[1..-1].replace(":", "/_3A_").toString())
        if (!dir.directory)
            return null
        for (name in dir.list({ _, n -> n.endsWith('.txt')} as FilenameFilter)) {
            def mediaType = mediaTypeMap[name[0..-5]]
            if (mediaType) {
                return [file: new File(dir, name), mediaType: mediaType]
            }
        }
    }

    def mediaTypeMap = [
        'txt': 'text/plain',
        'html': 'text/html',
        'xhtml': 'application/xhtml+xml',
        'pdf': 'application/pdf',
    ]

}
