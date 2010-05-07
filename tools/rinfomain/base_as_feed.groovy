
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.xml.sax.InputSource

import org.apache.commons.io.FileUtils as FU
import org.apache.commons.io.IOUtils

import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.ext.history.FeedPagingHelper

import org.openrdf.rio.RDFFormat
import org.openrdf.model.impl.URIImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.OWL

@Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
import se.lagrummet.rinfo.base.rdf.RDFUtil


DCT = "http://purl.org/dc/terms/"
VOID = "http://rdfs.org/ns/void#"
//DCT_IDENTIFIER = new URIImpl(DCT+"identifier")
DCT_HAS_PART = new URIImpl(DCT+"hasPart")
VOID_DATASET = new URIImpl(VOID+"Dataset")

// TODO: pass xslt dir to script..
GRIT_XSLT = "../../resources/external/xslt/grit/rdfxml-grit.xslt"
MODEL_XSLT = "../../resources/external/xslt/vocab/grit_to_xhtml.xslt"

FEED_META = [
    feedUri: "http://admin.lagrummet.se/base/feed",
    feedTitle: "RInfo Base Data",
    publicBaseUri: "http://rinfo.lagrummet.se"
]

// FIXME: cumbersome to make it work locally now (when statically served under "/admin" )
def feedPathConf(localBase="") {
    return [
        baseUrl: "${localBase}",
        feedPath: "${localBase}/feed/current"
    ]
}


def main() {
    def cli = new CliBuilder(usage:"groovy <script> [opts]")
    cli.h 'help', args:0
    cli.b 'base', args:1
    cli.s 'sources', args:1
    cli.o 'outdir', args:1
    cli.l 'localBase', args:1
    def opt = cli.parse(args)
    if (opt.h) {
        cli.usage(); System.exit 0
    }

    def base = opt.b ?: "../../resources/base/"
    def sources = opt.s ?: null
    def outdir = opt.o ?: "../../_build/rinfo-admin"
    def localBase = opt.l ?: ""
    assert outdir != null

    def items = collectItems(FEED_META.publicBaseUri, base, sources)
    def coll = createAtomCollection(FEED_META, feedPathConf(localBase), items)

    def extMap = [
        "application/rdf+xml": "rdf",
        "application/xhtml+xml": "xhtml",
        "application/atom+xml": "atom"
    ]
    coll.each { href, repr ->
        def fpath = outdir+href
        def ext = extMap[repr.mediaType]
        if (ext && !fpath.endsWith("."+ext)) {
            fpath += "."+ext
        }
        println fpath
        def f = new File(fpath)
        FU.forceMkdir(f.parentFile)
        f.withOutputStream {
            // TODO: can't all data results directly write to it instead?
            def ins = repr.data()
            IOUtils.copy(ins, it)
            ins.close()
        }
    }
}

//======================================================================

def collectItems(baseUri, base, sources) {
    def items = []
    def addItem = {
        if (it) items << it
    }

    FU.iterateFiles(new File(base, "model"),
            ["n3"] as String[], true).each {
        addItem modelItem(baseUri, it)
    }

    // Won't work - uses external model URI as atom:id (collector won't store those)
    //FU.iterateFiles(new File(base, "../external/rdf"),
    //        ["rdfs", "owl"] as String[], true).each {
    //    addItem modelItem(it)
    //}
    addItem datasetItem(baseUri, "ext/models",
            FU.listFiles(new File(base, "../external/rdf"),
                ["rdfs", "owl"] as String[], true))

    // .. "ext/model_translations"?
    addItem datasetItem(baseUri, "ext/extended_modeldata",
            FU.listFiles(new File(base, "extended/rdf"),
                ["n3"] as String[], true))

    addItem datasetItem(baseUri, "org",
            FU.listFiles(new File(base, "datasets/org"),
                ["n3"] as String[], true))

    addItem datasetItem(baseUri, "serie",
            FU.listFiles(new File(base, "datasets/serie"),
                ["n3"] as String[], true))

    addItem datasetItem(baseUri, "sys/uri", [
            new File(base, "sys/uri/scheme.n3"),
            new File(base, "sys/uri/slugs.n3")
        ])

    if (sources) {
        addItem simpleItem(baseUri, "sys/sources",
                new File(sources, "sys/sources.rdf"))
    } else {
        addItem simpleItem(baseUri, "sys/sources",
                new File(base, "datasets/feeds.n3"))
    }

    def vocabCssFile = new File("../../resources/external/xslt/vocab/css/vocab.css")
    def mediaEnclosures = [
            [ href: "/media/css/vocab.css",
              data: { new FileInputStream(vocabCssFile) },
              mediaType: "text/css" ]
        ]
    addItem( [
            uri: "${baseUri}/media",
            updated: new Date(vocabCssFile.lastModified()),
            content: "Media-filer",
            enclosures: mediaEnclosures
        ])

    return items
}

def simpleItem(baseUri, uriPath, File file) {
    def itemUri = baseUri+"/"+uriPath
    return [
        uri: itemUri,
        updated: new Date(file.lastModified()),
        content: [
            data: managedRdfInputStream(file, null, false),
            mediaType: "application/rdf+xml"
        ],
        enclosures: null
    ]
}

def modelItem(baseUri, File file) {
    def repo = RDFUtil.createMemoryRepository()
    def conn = repo.connection
    RDFUtil.loadDataFromFile(repo, file)
    def modelUri = getModelUri(conn)
    conn.close()
    if (modelUri == null) {
        repo.shutDown()
        return null
    }
    def modelUriStr = modelUri.toString()
    assert modelUriStr.startsWith(baseUri)
    def htmlReprSlug = modelUriStr.replaceAll(
            /${baseUri}(.+?)[#\/]?$/) { match, slug -> "${slug}/xhtml" }
    return [
        uri: modelUri as String,
        updated: new Date(file.lastModified()),
        content: [
            data: managedRdfInputStream(file, repo),
            mediaType: "application/rdf+xml"
        ],
        alternate: [
            [ href: htmlReprSlug,
              data: { rdfVocabToXhtml(managedRdfInputStream(file, repo)()) },
              mediaType: "application/xhtml+xml" ]
        ]
    ]
}

def datasetItem(baseUri, uriPath, List<File> files) {
    def itemUri = baseUri+"/"+uriPath
    def enclosures = []
    def youngestEnclDate = null

    def repo = RDFUtil.createMemoryRepository()
    def conn = repo.connection
    def vf = repo.valueFactory
    conn.setNamespace("dct", DCT)
    conn.setNamespace("void", VOID)

    def itemRdfUri = vf.createURI(itemUri)
    conn.add(itemRdfUri, RDF.TYPE, VOID_DATASET)
    files.each { file ->
        def slug = "/"+uriPath+"/"+file.name.replace(".n3", ".rdf")
        conn.add(itemRdfUri, DCT_HAS_PART, vf.createURI(baseUri+slug))
        enclosures << [
            href: slug,
            data: managedRdfInputStream(file),
            mediaType: "application/rdf+xml"
        ]
        def fileDate = new Date(file.lastModified())
        if (!youngestEnclDate || fileDate > youngestEnclDate) {
            youngestEnclDate = fileDate
        }
    }
    conn.close()
    return [
        uri: itemUri,
        updated: youngestEnclDate,
        content: [data: { repoToInStream(repo, true) },
                  mediaType: "application/rdf+xml"],
        enclosures: enclosures
    ]
}

def getModelUri(conn) {
    def uri = null
    RDFUtil.one(conn, null, RDF.TYPE, OWL.ONTOLOGY, true).each {
        uri = it.subject
    }
    return uri
}

/**
 * Either uses the file as-is and closes the repo, or returns a lazy serializer..
 */
Closure managedRdfInputStream(file, final repo=null, parse=false) {
    if (parse || file.name.endsWith(".n3")) {
        if (repo == null) {
            repo = RDFUtil.createMemoryRepository()
            RDFUtil.loadDataFromFile(repo, file)
        }
        // TODO: skip closure and serialize directly?
        return {
            def ins = repoToInStream(repo)
            //repo.shutDown()
            return ins
        }
    } else {
        return { new FileInputStream(file) }
    }
}

def repoToInStream(repo, pretty=true) {
    return RDFUtil.toInputStream(repo, RDFFormat.RDFXML.defaultMIMEType, pretty)
}

//======================================================================

SAX_F = javax.xml.parsers.SAXParserFactory.newInstance()
SAX_F.setNamespaceAware(true)
SAX_TF = (SAXTransformerFactory) TransformerFactory.newInstance()

def makeModelToXhtmlTransformerHandler(result) {
    gritHandler = SAX_TF.newTransformerHandler(new StreamSource(GRIT_XSLT))
    vocabHandler = SAX_TF.newTransformerHandler(new StreamSource(MODEL_XSLT))
    // TODO: should be "/media", this is just to handle the local "/admin" test case..
    vocabHandler.transformer.setParameter("mediabase", "../../../../../media")
    xhtmlHandler = SAX_TF.newTransformerHandler()
    [   (OutputKeys.METHOD): "xml",
        (OutputKeys.OMIT_XML_DECLARATION): "yes",
        (OutputKeys.DOCTYPE_PUBLIC): "-//W3C//DTD XHTML 1.0 Strict//EN",
        (OutputKeys.DOCTYPE_SYSTEM): "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
    ].each { k, v ->
        xhtmlHandler.transformer.setOutputProperty(k, v)
    }
    gritHandler.setResult(new SAXResult(vocabHandler))
    vocabHandler.setResult(new SAXResult(xhtmlHandler))
    xhtmlHandler.setResult(result)
    return gritHandler
}

InputStream rdfVocabToXhtml(inputStream) {
    def reader = SAX_F.newSAXParser().getXMLReader()
    return outputToInputStream {
        reader.setContentHandler(
                makeModelToXhtmlTransformerHandler(new StreamResult(it)))
        reader.parse(new InputSource(inputStream))
    }
}

//======================================================================

Map createAtomCollection(feedMeta, feedPathConf, items) {
    def collection = [:]

    def feed = Abdera.instance.newFeed()
    feed.id = feedMeta.feedUri
    feed.setTitle(feedMeta.feedTitle)
    FeedPagingHelper.setComplete(feed, true)
    def youngestUpdated = null

    for (item in items) {
        def updated = item.updated
        if (!youngestUpdated || updated > youngestUpdated) {
            youngestUpdated = updated
        }
        def entry = Abdera.instance.newEntry()
        entry.setId(item.uri)
        entry.setTitle(item.uri)
        entry.setUpdated(updated)
        if (item.content instanceof String) {
            entry.setContent(item.content)
        } else {
            def contentHref = makeHref(
                    feedMeta.publicBaseUri, feedPathConf.baseUrl, item.uri, "rdf")
            collection[contentHref] = item.content
            entry.setContent(new IRI(contentHref), item.content.mediaType)
        }

        for (encl in item.alternate) {
            def href = feedPathConf.baseUrl + encl.href
            collection[href] = encl
            entry.addLink(href, "alternate", encl.mediaType, null, null, -1)
        }

        for (encl in item.enclosures) {
            // TODO: when abdera *parses* this (in collector), it seems to
            // *remove* "mismatch" of fileext and mediaType!
            // .. cannot reproduce.. Trace..
            def href = feedPathConf.baseUrl +
                    encl.href.replace(".owl", ".rdf").replace(".rdfs", ".rdf")

            collection[href] = encl
            entry.addLink(href, "enclosure", encl.mediaType,
                    null/*title*/, null/*lang*/, -1)
        }
        feed.insertEntry(entry)
    }
    feed.setUpdated(youngestUpdated ?: new Date())
    feed.sortEntriesByUpdated(true)

    collection[feedPathConf.feedPath] = [
        data: {
            return outputToInputStream { feed.writeTo("prettyxml", it) }
        },
        mediaType: "application/atom+xml"
    ]
    return collection
}

def makeHref(publicBaseUri, baseUrl, uri, ext) {
    return uri.replace(publicBaseUri, baseUrl).replace('#', '')+"/"+ext
}

def outputToInputStream(Closure closure) {
    def bos = new ByteArrayOutputStream()
    try { closure(bos) } finally { bos.close() }
    return new ByteArrayInputStream(bos.toByteArray())
}

//======================================================================

main()

