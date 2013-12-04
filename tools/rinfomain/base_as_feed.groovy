
@Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
@Grab('org.restlet.jse:org.restlet:2.0.9')
@Grab('org.restlet.jee:org.restlet.ext.servlet:2.0.9')
//@Grab('rdfa:rdfa-sesame:0.1.0-SNAPSHOT')
@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')

import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.xml.sax.InputSource

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.ext.history.FeedPagingHelper

import org.openrdf.model.impl.URIImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.OWL

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer


DCT = "http://purl.org/dc/terms/"
VOID = "http://rdfs.org/ns/void#"
DCT_HAS_PART = new URIImpl(DCT+"hasPart")
VOID_DATASET = new URIImpl(VOID+"Dataset")

// IMPROVE: pass xslt dir to script..
GRIT_XSLT = "../../resources/external/xslt/grit/rdfxml-grit.xslt"
MODEL_XSLT = "../../resources/external/xslt/vocab/grit_to_xhtml.xslt"

FEED_META = [
    feedId: "tag:lagrummet.se,2010:rinfo:admin",
    feedTitle: "RInfo Base Data",
    publicBaseUri: "http://rinfo.lagrummet.se"
]

// IMPROVE: cumbersome to make it work locally now (when statically served under "/admin" )
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
    cli.nocomplete 'do not mark Atom feed as complete'
    def opt = cli.parse(args)
    if (opt.h) {
        cli.usage(); System.exit 0
    }

    def base = opt.b ?: "../../resources/base/"
    def outdir = opt.o ?: "../../_build/rinfo-admin"
    def localBase = opt.l ?: ""
    def sources = opt.s ?: null
    def markComplete = !opt.nocomplete
    assert outdir != null

    def items = collectItems(FEED_META.publicBaseUri, base, sources)
    def coll = createAtomCollection(FEED_META, feedPathConf(localBase), items, markComplete)

    def extMap = [
        "application/rdf+xml": "rdf",
        "application/xhtml+xml": "xhtml",
        "application/atom+xml": "atom",
        "text/html": "html",
    ]
    println "------------------------- Main loop ------------------"
    coll.each { href, repr ->
        def fpath = outdir+href
        println "fpath="+fpath
        def ext = extMap[repr.mediaType]
        if (ext && !fpath.endsWith("."+ext)) {
            fpath += "."+ext
        }
        println fpath
        def f = new File(fpath)
        FileUtils.forceMkdir(f.parentFile)
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
    def listFiles = { dirPath, suffixes ->
        FileUtils.listFiles(new File(dirPath), suffixes as String[], true)
    }

    listFiles("${base}/model", ["n3"]).each {
        addItem modelItem(baseUri, it)
    }

    addItem datasetItem(baseUri, "ext/vocab",
            listFiles("${base}/../external/rdf", ["rdfs", "owl"]))

    addItem datasetItem(baseUri, "ext/vocab/extras",
            listFiles("${base}/extended/rdf", ["n3"]))

    listFiles("${base}/datasets/org", ["n3"]).each {
        getSimpleItemsByType(baseUri, it).each addItem
    }

    listFiles("${base}/datasets/serie", ["n3"]).each {
        getSimpleItemsByType(baseUri, it).each addItem
    }

    addItem datasetItem(baseUri, "sys/uri", [
            new File(base, "sys/uri/space.n3"),
            new File(base, "sys/uri/slugs.n3")
        ])

    addItem simpleItem(baseUri, "sys/validation",
            new File(base, "validation/index.ttl"),
            listFiles("${base}/validation", ["rq"]).collect {[
                    href: "/sys/validation/${it.name}",
                    writeTo: createFileWriter(it),
                    mediaType: "application/sparql-query"
                ]})

    if (sources) {
        def sourceFile = new File(sources)
        if (sourceFile.directory)
            sourceFile = new File(sourceFile, "sys/dataset.rdf")
        addItem simpleItem(baseUri, "sys/dataset", sourceFile)
    } else {
        addItem simpleItem(baseUri, "sys/dataset",
                new File(base, "datasets/sources.n3"))
    }

    addItem mediaItem("${baseUri}/media", "Media-filer", [
                [ href: "/media/css/vocab.css",
                    file: new File("${base}/../external/xslt/vocab/css/vocab.css"),
                    mediaType: "text/css" ] ])

    return items
}


def simpleItem(baseUri, uriPath, File file, enclosures=null) {
    def itemUri = baseUri+"/"+uriPath
    return [
        uri: itemUri,
        updated: new Date(file.lastModified()),
        content: [
            writeTo: createRdfWriter(file, null),
            mediaType: "application/rdf+xml"
        ],
        enclosures: enclosures
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
    def modelDocUri = modelUriStr.replaceFirst(/#.*$/, '') // strip trailing fragment
    return [
        uri: modelDocUri.toString(),
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


def getSimpleItemsByType(String baseUri, File dataFile) {
    def items = []
    def repo = RDFUtil.createMemoryRepository()
    RDFUtil.loadDataFromFile(repo, dataFile)
    def conn = repo.getConnection()
    def describer = new Describer(conn).setPrefix("http", "http:")
    try {
        def types = describer.objectValues(null, "rdf:type")
        types.each {
            describer.getByType(it).each {
                def iri = it.about
                assert iri.startsWith(baseUri)
                def itemRepo = RDFUtil.constructQuery(conn, """CONSTRUCT {
                            ?item ?p ?o .
                            ?o ?p2 ?o2 .
                        } WHERE {
                            ?item ?p ?o .
                            OPTIONAL { ?o ?p2 ?o2 . } } """,
                        [item: conn.valueFactory.createURI(iri)])
                def itemConn = itemRepo.getConnection()
                def iter = conn.namespaces
                while (iter.hasNext()) {
                    def ns = iter.next()
                    itemConn.setNamespace(ns.prefix, ns.name)
                }
                items << [
                    uri: iri,
                    updated: new Date(dataFile.lastModified()),
                    content: [writeTo: repoWriter(itemRepo),
                            mediaType: "application/rdf+xml"]
                ]
            }
        }
    } finally {
        conn.close()
    }
    return items
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
Closure createRdfWriter(file, repo=null) {
    if (file.name.endsWith(".n3") || file.name.endsWith(".ttl")) {
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
        RDFUtil.serialize(repo, RDFUtil.RDF_XML, it, true)
        //repo.shutDown()
    }
}

InputStream repoToInStream(repo, pretty=true) {
    return RDFUtil.toInputStream(repo, RDFUtil.RDF_XML, pretty)
}

Closure createFileWriter(file) {
    return { outStream ->
        file.withInputStream {
            IOUtils.copy(it, outStream)
        }
    }
}

//======================================================================

SAX_F = javax.xml.parsers.SAXParserFactory.newInstance("org.apache.xerces.jaxp.SAXParserFactoryImpl", null)
SAX_F.setNamespaceAware(true)
SAX_TF = (SAXTransformerFactory) TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl",null)


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

Map createAtomCollection(feedMeta, feedPathConf, items, markComplete=true) {
    def collection = [:]

    def feed = Abdera.instance.newFeed()
    feed.id = feedMeta.feedId
    feed.setTitle(feedMeta.feedTitle)
    if (markComplete)
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

