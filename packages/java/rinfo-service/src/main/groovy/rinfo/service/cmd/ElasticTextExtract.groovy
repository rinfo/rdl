package rinfo.service.cmd

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.index.query.QueryBuilders


def host = "127.0.0.1"
def port = 9300
indexName = args[0]
textDir = args[1] as File

client = new TransportClient().addTransportAddress(
                new InetSocketTransportAddress(host, port))

def runQuery(startIndex, itemsPerPage) {
    def srb = client.prepareSearch(indexName)
    def qb = QueryBuilders.queryString("hasFormat.text:*")
    srb.addFields("iri", "hasFormat.text", "hasFormat.format")
    srb.setQuery(qb)
    srb.setFrom(startIndex)
    srb.setSize(itemsPerPage)
    return srb.execute().actionGet()
}

void handleResults(esRes) {
    esRes.hits.hits.each {
        def iri = it.fields.iri.value
        def text = it.fields['hasFormat.text'].value
        def mediaType = it.fields['hasFormat.format'].value
        println "<${iri}> (${text.size()} bytes)"
        saveContent(iri, text, mediaType)
    }
}

void saveContent(iri, text, mediaType) {
    def path = new URI(iri).path[1..-1].replace(":", "/_3A_") +
            '/' + mediaExt[mediaType] + '.txt'
    new File(textDir, path).with {
        if (!parentFile.directory) parentFile.mkdirs()
        setText(text, "UTF-8")
    }
}

mediaExt = [
    'text/plain': 'txt',
    'text/html': 'html',
    'application/xhtml+xml': 'xhtml',
    'application/pdf': 'pdf',
]


def startIndex = 0
def itemsPerPage = 50
def visited = 0
def totalResults = -1
while (true) {
    def esRes = runQuery(startIndex, itemsPerPage)
    totalResults = esRes.hits.totalHits()
    handleResults(esRes)
    visited += esRes.hits.hits.length
    if (esRes.hits.hits.length == itemsPerPage &&
            (startIndex + itemsPerPage) < totalResults) {
        startIndex += itemsPerPage
        println "At ${visited} items..."
        continue
    } else {
        break
    }
}
assert visited == totalResults
println "Done (visited ${visited} items)."

