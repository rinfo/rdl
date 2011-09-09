package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j

import org.elasticsearch.client.Client
import org.elasticsearch.action.WriteConsistencyLevel
import org.elasticsearch.client.action.index.IndexRequestBuilder

import se.lagrummet.rinfo.base.rdf.jsonld.JSONLDSerializer


@Slf4j
class ElasticLoader {

    Client client
    String indexName

    def textExtractor = new TextExtractor()
    def contentMediaTypes = ["application/pdf",
        "application/xhtml+xml", "text/html", "text/plain"]

    ElasticLoader(client, indexName) {
        this.client = client
        this.indexName = indexName
    }

    void create(entry) {
        def id = entry.id
        def data = toElasticData(entry) // + repoEntry...
        if (data == null) {
            log.info "No elastic data created for <${entry.id}> - skipped."
            return
        }
        log.info "Indexing elastic data for <${id}>..."
        IndexRequestBuilder irb = client.prepareIndex(indexName, indexType, id).
            setConsistencyLevel(WriteConsistencyLevel.DEFAULT).
            setSource(data)
        irb.execute().actionGet()
        log.info "Done."
    }

    void delete(entryId) {
    }

    Map toElasticData(entry) {
        return null /* TODO
        def rq = "sparql/construct_summary.rq"
        new JSONLDSerializer(conn, false, false)
        def contentRef = null
        def contents = entry.findContents()
        // TODO: but we don't expect multiple, but if there are, which one to use? warn?
        for (content in contents) {
          if (content.mediaType in contentMediaTypes) {
            contentRef = content
            break
          }
        }
        if (contentRef) {
            println "Adding document data with mediaType ${contentRef.mediaType}"
            data['document'] = [
                'content_type': contentRef.mediaType,
                'content': textExtractor.getString(contentRef.file)
            ]
        }
        */
    }

}
