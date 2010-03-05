import javax.xml.namespace.QName

import org.apache.commons.io.FileUtils as FU
import org.apache.commons.io.IOUtils

import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI

import org.openrdf.rio.RDFFormat
import org.openrdf.model.impl.URIImpl
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.RDFS
import org.openrdf.model.vocabulary.OWL

import se.lagrummet.rinfo.base.rdf.RDFUtil


DCT = "http://purl.org/dc/terms/"
VOID = "http://rdfs.org/ns/void#"
//DCT_IDENTIFIER = new URIImpl(DCT+"identifier")
DCT_HAS_PART = new URIImpl(DCT+"hasPart")
VOID_DATASET = new URIImpl(VOID+"Dataset")


FEED_META = [
    feedUri: "http://admin.lagrummet.se/base/feed",
    feedTitle: "RInfo Base Data",
    publicBaseUri: "http://rinfo.lagrummet.se"
]

FEED_PATH_CONF = [
    baseUrl: "",
    feedPath: "/feed/current"
]


@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
@Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
def main() {
    def cli = new CliBuilder(usage:"groovy <script> [opts]")
    cli.h 'help', args:0
    cli.b 'base', args:1
    cli.s 'sources', args:1
    cli.o 'outdir', args:1
    def opt = cli.parse(args)
    if (opt.h) {
        cli.usage(); System.exit 0
    }

    def base = opt.b ?: "../../resources/base/"
    def sources = opt.s ?: null
    def outdir = opt.o
    assert outdir != null

    def items = collectItems(FEED_META.publicBaseUri, base, sources)
    def coll = createAtomCollection(FEED_META, FEED_PATH_CONF, items)

    def extMap = [
        "application/rdf+xml": "rdf",
        "application/atom+xml": "atom"
    ]
    coll.each { href, repr ->
        def fpath = outdir+href
        def ext = "."+extMap[repr.mediaType]
        if (!fpath.endsWith(ext)) {
            fpath += ext
        }
        println fpath
        def f = new File(fpath)
        FU.forceMkdir(f.parentFile)
        f.withOutputStream {
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
        addItem modelItem(it)
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
            new File(base, "sys/uri/symbol/org.n3"),
            new File(base, "sys/uri/symbol/serie.n3")
        ])

    if (sources) {
        addItem simpleItem(baseUri, "sys/sources",
                new File(sources, "sys/sources.rdf"))
    } else {
        addItem simpleItem(baseUri, "sys/sources",
                new File(base, "datasets/feeds.n3"))
    }

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

def modelItem(File file) {
    def repo = RDFUtil.createMemoryRepository()
    def conn = repo.connection
    RDFUtil.loadDataFromFile(repo, file)
    def modelUri = getModelUri(conn)
    //def walker = new SesameRDFWalker(conn, about:modelUri)
    //def enclosures = walker.rel(OWL.IMPORTS).collect {
    //    [ href: it as String, mediaType: "application/rdf+xml" ]
    //}
    conn.close()
    if (modelUri == null) {
        repo.shutDown()
        return null
    }
    return [
        uri: modelUri as String,
        updated: new Date(file.lastModified()),
        content: [
            data: managedRdfInputStream(file, repo),
            mediaType: "application/rdf+xml"
        ],
        enclosures: null
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

def createAtomCollection(feedMeta, feedPathConf, items) {
    def collection = [:]

    def feed = Abdera.instance.newFeed()
    feed.id = feedMeta.feedUri
    feed.setTitle(feedMeta.feedTitle)
    def youngestUpdated = null

    for (item in items) {
        def updated = item.updated
        if (!youngestUpdated || updated > youngestUpdated) {
            youngestUpdated = updated
        }
        def contentHref = makeHref(
                feedMeta.publicBaseUri, feedPathConf.baseUrl, item.uri, "rdf")
        collection[contentHref] = item.content
        def entry = Abdera.instance.newEntry()
        entry.setId(item.uri)
        entry.setTitle(item.uri)
        entry.setUpdated(updated)
        entry.setContent(new IRI(contentHref), item.content.mediaType)

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

    collection[feedPathConf.feedPath] = [
        data: {
            def bos = new ByteArrayOutputStream()
            feed.writeTo("prettyxml", bos)
            bos.close()
            return new ByteArrayInputStream(bos.toByteArray())
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

