package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j as Log

import org.elasticsearch.client.Client
import org.elasticsearch.action.WriteConsistencyLevel
import org.elasticsearch.client.action.index.IndexRequestBuilder

import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.jsonld.JSONLDSerializer


@Log
class ElasticLoader {

    ElasticData elasticData

    String constructSummaryQuery

    def textExtractor = new TextExtractor()
    def contentMediaTypes = ["application/pdf",
        "application/xhtml+xml", "text/html", "text/plain"]

    ElasticLoader(elasticData) {
        this.elasticData = elasticData
        this.constructSummaryQuery = getClass().getResourceAsStream(
                    "/sparql/construct_summary.rq").getText("utf-8")
    }

    void create(RepositoryConnection conn, entry, collector) {
        def id = entry.id.toString()
        def docType = findElasticType(entry.id.toURI())
        if (docType == null) {
            log.info "No elastic type detected for <${entry.id}> - skipping."
            return
        }
        def data = toElasticData(conn, entry, collector)
        if (data == null) {
            log.info "No elastic data created for <${entry.id}> - skipping."
            return
        }
        log.info "Indexing elastic data of doctype ${docType} with id <${id}>..."
        // TODO: ensure that updates have effect!
        IndexRequestBuilder irb = elasticData.client.prepareIndex(elasticData.indexName, docType, id).
            setConsistencyLevel(WriteConsistencyLevel.DEFAULT).
            setSource(data)
        irb.execute().actionGet()
        log.info "Done."
    }

    void delete(URI entryId) {
        def docType = findElasticType(entry.id)
        //DeleteResponse response =
        elasticData.client.prepareDelete(
                elasticData.indexName, docType, entryId.toString()).execute().actionGet()
    }

    String findElasticType(URI uri) {
        def pathNoLead = uri.path.substring(1)
        int slashPos = pathNoLead.indexOf("/")
        if (slashPos == -1) return null
        return pathNoLead.substring(0, slashPos)
    }

    Map toElasticData(conn, entry, collector) {
        def resourceUri = entry.id.toString()
        def summaryRepo = getSummaryRDF(conn, resourceUri)
        if (summaryRepo == null)
            return null
        def jsonLdSerializer = elasticData.jsonLdSettings.createJSONLDSerializer()
        def data = jsonLdSerializer.toJSON(summaryRepo, resourceUri)
        if (data) {
            cleanForElastic(data)
            addContent(data, entry, collector)
        }
        return data
    }

    Repository getSummaryRDF(conn, String resourceUri) {
        return RDFUtil.constructQuery(conn, constructSummaryQuery,
                ["current": conn.valueFactory.createURI(resourceUri)])
    }

    /**
     * Removes string values from lists mixing strings and maps. If the result
     * has size 1, use single value.
     */
    static def cleanForElastic(data, refKeys=['creator']) {
        for (key in refKeys) {
            if (data[key] instanceof String) {
                log.info "Cleaning key ${key}: removing string where expected reference."
                data.remove(key)
            }
        }
        data.each { key, value ->
            if (value instanceof Map) {
                cleanForElastic(value)
            } else if (value instanceof List) {
                if (value.find { it instanceof Map } &&
                        value.find { !(it instanceof Map) }) {
                    log.info "Cleaning key ${key}: found Maps in list, removing other values."
                    def mapValues = value.findAll { it instanceof Map }
                    data[key] = (mapValues.size() == 1)? mapValues[0] : mapValues
                }
            }
        }
    }

    void addContent(data, entry, collector) {
        def contentRef = findContentRef(entry)
        if (contentRef) {
            log.info "Adding document data with mediaType ${contentRef.mediaType}"
            def contentText = getContentText(contentRef.url, collector)
            setContent(data, contentText, contentRef.mediaType)
        }
    }

    def findContentRef(entry) {
        // TODO: we don't expect multiple content docs, but if there are, which one to use? warn?
        def contentElem = entry.contentElement
        if (contentElem?.resolvedSrc) {
            def contentUrl = contentElem.resolvedSrc.toString()
            def contentMediaType = contentElem.mimeType.toString()
            if (contentMediaTypes.contains(contentMediaType)) {
                return [
                    mediaType: contentMediaType,
                    url: contentUrl
                ]
            }
        }
        for (link in entry.links) {
            def urlPath = link.resolvedHref.toString()
            def mediaType = link.getMimeType().toString()
            if (link.rel == "alternate") {
                if (contentMediaTypes.contains(mediaType)) {
                    return [
                        mediaType: mediaType,
                        url: urlPath
                    ]
                }
            }
        }
    }

    String getContentText(String url, collector) {
        def inputStream = collector.getResponseAsInputStream(url)
        def contentText = null
        try {
            contentText = textExtractor.getText(inputStream)
        } finally {
            inputStream.close()
        }
        return contentText
    }

    void setContent(data, contentText, mediaType) {
        data['document'] = [
            'content_type': mediaType,
            'content': contentText
        ]
    }

}
