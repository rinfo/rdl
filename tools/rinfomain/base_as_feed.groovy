
@Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')

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

import se.lagrummet.rinfo.base.rdf.RDFUtil


DCT = "http://purl.org/dc/terms/"
VOID = "http://rdfs.org/ns/void#"
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
        "application/atom+xml": "atom",
        "text/html": "html",
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
            repr.writeTo(it)
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

    addItem datasetItem(baseUri, "ext/models",
            FU.listFiles(new File(base, "../external/rdf"),
                ["rdfs", "owl"] as String[], true))

    // TODO: "ext/model_translations"?
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

    addItem mediaItem("${baseUri}/media", "Media-filer", [
                [ href: "/media/css/vocab.css",
                    file: new File("${base}/../external/xslt/vocab/css/vocab.css"),
                    mediaType: "text/css" ] ])

    return items
}


def simpleItem(baseUri, uriPath, File file) {
    def itemUri = baseUri+"/"+uriPath
    return [
        uri: itemUri,
        updated: new Date(file.lastModified()),
        content: [
            writeTo: createRdfWriter(file, null),
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
            writeTo: createRdfWriter(file, repo),
            mediaType: "application/rdf+xml"
        ],
        alternate: [
            [ href: htmlReprSlug,
              writeTo: { rdfVocabToXhtml(repoToInStream(repo), it) },
              mediaType: "text/html" ]
        ]
    ]
}

def getModelUri(conn) {
    def uri = null
    RDFUtil.one(conn, null, RDF.TYPE, OWL.ONTOLOGY, true).each {
        uri = it.subject
    }
    return uri
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
            writeTo: createRdfWriter(file),
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
        content: [writeTo: repoWriter(repo),
                  mediaType: "application/rdf+xml"],
        enclosures: enclosures
    ]
}


def mediaItem(itemUri, content, mediaIitems) {
    def youngestEnclDate = null
    def enclosures = []
    mediaIitems.each { item ->
        enclosures << [
            href: item.href,
            writeTo: createFileWriter(item.file),
            mediaType: item.mediaType
        ]
        def fileDate = new Date(item.file.lastModified())
        if (!youngestEnclDate || fileDate > youngestEnclDate) {
            youngestEnclDate = fileDate
        }
    }
    return [
        uri: itemUri,
        updated: youngestEnclDate,
        content: "Media-filer",
        //content: [writeTo: { it << "Media-filer" },
        //          mediaType: "text/plain"],
        enclosures: enclosures
    ]
}


//======================================================================

/**
 * Either uses the file as-is and closes the repo, or returns a lazy serializer..
 */
Closure createRdfWriter(file, final repo=null) {
    if (file.name.endsWith(".n3")) {
        if (repo == null) {
            repo = RDFUtil.createMemoryRepository()
            RDFUtil.loadDataFromFile(repo, file)
        }
        return repoWriter(repo)
    } else {
        return createFileWriter(file)
    }
}

Closure repoWriter(repo, pretty=true) {
    return {
        RDFUtil.serialize(repo, RDFFormat.RDFXML.defaultMIMEType, it, true)
        //repo.shutDown()
    }
}

InputStream repoToInStream(repo, pretty=true) {
    return RDFUtil.toInputStream(repo, RDFFormat.RDFXML.defaultMIMEType, pretty)
}

Closure createFileWriter(file) {
    return { outStream ->
        file.withInputStream {
            IOUtils.copy(it, outStream)
        }
    }
}

//======================================================================

SAX_F = javax.xml.parsers.SAXParserFactory.newInstance()
SAX_F.setNamespaceAware(true)
SAX_TF = (SAXTransformerFactory) TransformerFactory.newInstance()

InputStream rdfVocabToXhtml(inputStream, outputStream) {
    def reader = SAX_F.newSAXParser().getXMLReader()
    reader.setContentHandler(
            makeModelToXhtmlTransformerHandler(new StreamResult(outputStream)))
    reader.parse(new InputSource(inputStream))
}

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
        writeTo: {
            feed.writeTo("prettyxml", it)
        },
        mediaType: "application/atom+xml"
    ]
    return collection
}

def makeHref(publicBaseUri, baseUrl, uri, ext) {
    return uri.replace(publicBaseUri, baseUrl).replace('#', '')+"/"+ext
}

//======================================================================

main()

