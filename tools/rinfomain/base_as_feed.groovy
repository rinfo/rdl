import org.apache.commons.io.FileUtils as FU

import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI

import org.openrdf.rio.RDFFormat
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.RDFS
import org.openrdf.model.vocabulary.OWL

import se.lagrummet.rinfo.base.rdf.RDFUtil

import org.restlet.Application
import org.restlet.Component
import org.restlet.Directory
import org.restlet.Restlet
import org.restlet.Server
import org.restlet.data.MediaType as MT
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.resource.InputRepresentation


FEED_URI = "http://rinfo-admin.lagrummet.se/base/feed"
FEED_TITLE = "RInfo Base Data"
PUBLIC_BASE_URI = "http://rinfo.lagrummet.se"
BASE_PATH = "/admin/feed/current"

@Grab(group='se.lagrummet.rinfo', module='rinfo-base', version='1.0-SNAPSHOT')
@Grab(group='se.lagrummet.rinfo', module='rinfo-store', version='1.0-SNAPSHOT')
def main() {
    def cli = new CliBuilder(usage:"groovy <script> [opts]")
    cli.h 'help', args:0
    cli.b 'base', args:1
    cli.s 'sources', args:1
    cli.p 'port', args:1
    def opt = cli.parse(args)
    if (opt.h) { cli.usage(); System.exit 0 }

    def base = opt.b ?: "../../resources/base/"
    def sources = opt.s ?: null
    def port = Integer.parseInt(opt.p ?: "8280")

    Closure makeCollection = {
        def items = collectItems(base, sources, port)
        def coll = createAtomCollection(
                FEED_URI, FEED_TITLE, PUBLIC_BASE_URI, items)
    }

    if (port == -1) {
        def coll = makeCollection()
        //coll.each { k, v -> println "${k}: ${v}" }
        println coll[BASE_PATH].data().text
    } else {
        startServer port, makeCollection, sources
        // TODO: pingMainWithMe?
    }
}

//======================================================================

class BaseRestlet extends Restlet {
    Closure makeCollection
    def collection
    def basePath
    BaseRestlet(makeCollection, basePath) {
        this.makeCollection = makeCollection
        this.basePath = basePath
    }
    void handle(Request request, Response response) {
        def path = request.resourceRef.relativeRef.path as String
        if (collection == null || path == basePath) {
            collection = makeCollection()
        }
        def repr = collection[path]
        if (repr) {
            response.setEntity(
                new InputRepresentation(repr.data(), repr.mediaType) )
        }
    }
}

class SourceApp extends Application {
    String wwwDir
    Restlet createRoot() {
        return new Directory(context, wwwDir as String)
    }
}

def startServer(port, makeCollection, sources) {
    //def restlet = new BaseRestlet(makeCollection)
    //new Server(Protocol.HTTP, port, restlet).start()
    def component = new Component()
    component.servers.add(Protocol.HTTP, port)
    component.clients.add(Protocol.FILE)
    component.defaultHost.attach(new BaseRestlet(makeCollection, BASE_PATH))
    if (sources) {
        new File(sources).listFiles({it.isDirectory()} as FileFilter).each {
            if (it.name != "sys") {
                component.defaultHost.attach("/${it.name}",
                        new SourceApp(wwwDir:it.toURI().toString()))
            }
        }
    }
    component.start()
}

//======================================================================
// TODO: passing too much state around, make class

def collectItems(base, sources, port) {
    def baseUri = "http://localhost:${port}/"

    def items = []
    def addItem = {
        if (it) items << it
    }

    FU.iterateFiles(new File(base, "model"),
            ["n3"] as String[], true).each {
        addItem modelItem(baseUri, it)
    }

    // Doesn't work - will use external model URI:s (collector won't load those).
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
    }

    return items
}

def simpleItem(baseUri, uriPath, File file) {
    def itemUri = PUBLIC_BASE_URI+"/"+uriPath
    return [
        uri: itemUri,
        updated: new Date(file.lastModified()),
        content: [
            data: managedRdfInputStream(baseUri, file, null, true),
            mediaType: MT.APPLICATION_RDF_XML
        ],
        enclosures: null
    ]
}

def modelItem(baseUri, File file) {
    def repo = RDFUtil.createMemoryRepository()
    def conn = repo.connection
    RDFUtil.loadDataFromFile(repo, file)
    def modelUri = getModelUri(conn)
    //def enclosures = []
    //collectObjects(conn, modelUri, OWL.IMPORTS).each {
    //    enclosures << [ href: it as String, mediaType: MT.APPLICATION_RDF_XML ]
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
            data: managedRdfInputStream(baseUri, file, repo),
            mediaType: MT.APPLICATION_RDF_XML
        ],
        enclosures: null
    ]
}

//def collectObjects(conn, uri, property) {
//    def stmts = conn.getStatements(uri, property, null, false);
//    def res = []
//    while (stmts.hasNext()) {
//        res << stmts.next().object
//    }
//    stmts.close()
//    return res
//}

def datasetItem(baseUri, uriPath, List<File> files) {
    def itemUri = PUBLIC_BASE_URI+"/"+uriPath
    def enclosures = []
    def youngestEnclDate = null

    def setRepo = RDFUtil.createMemoryRepository()
    def conn = setRepo.connection
    files.each { file ->
        def slug = "/"+uriPath+"/"+file.name.replace(".n3", ".rdf")

        conn.add(
                setRepo.valueFactory.createURI(itemUri),
                RDFS.SEEALSO,
                setRepo.valueFactory.createURI(PUBLIC_BASE_URI + slug))

        enclosures << [
            href: slug,
            data: managedRdfInputStream(baseUri, file),
            mediaType: MT.APPLICATION_RDF_XML
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
        content: [data: { repoToInStream(setRepo) }, mediaType: MT.APPLICATION_RDF_XML],
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
// TODO: Skip Closure; serialize directly?
Closure managedRdfInputStream(baseUri, file, final repo=null, parse=false) {
    if (parse || file.name.endsWith(".n3")) {
        if (repo == null) {
            repo = RDFUtil.createMemoryRepository()
            RDFUtil.loadDataFromFile(repo, file, baseUri, null)
        }
        return { repoToInStream(repo) }
        repo.shutDown()
    } else {
        return { new FileInputStream(file) }
    }
}

def repoToInStream(repo) {
    return RDFUtil.toInputStream(repo, RDFFormat.RDFXML.defaultMIMEType)
}

//======================================================================

def createAtomCollection(feedUri, feedTitle, baseUri, items) {
    def collection = [:]

    def feed = Abdera.instance.newFeed()
    feed.id = feedUri
    feed.setTitle(feedTitle)
    def youngestUpdated = null

    for (item in items) {
        def updated = item.updated
        if (!youngestUpdated || updated > youngestUpdated) {
            youngestUpdated = updated
        }
        def contentHref = makeHref(baseUri, item.uri, "rdf")
        collection[contentHref] = item.content
        def entry = Abdera.instance.newEntry()
        entry.setId(item.uri)
        entry.setTitle(item.uri)
        entry.setUpdated(updated)
        entry.setContent(new IRI(contentHref), item.content.mediaType as String)

        for (encl in item.enclosures) {

            // TODO: when abdera *parses* this (in collector), it seems to
            // *remove* "mismatch" of fileext and mediaType!
            // .. cannot reproduce.. Trace..
            def href = encl.href.replace(".owl", ".rdf").replace(".rdfs", ".rdf")

            collection[href] = encl
            entry.addLink(href, "enclosure", encl.mediaType as String,
                    null/*title*/, null/*lang*/, -1)
        }
        feed.insertEntry(entry)
    }
    feed.setUpdated(youngestUpdated ?: new Date())

    collection[BASE_PATH] = [
        data: {
            def bos = new ByteArrayOutputStream()
            feed.writeTo("prettyxml", bos)
            bos.close()
            return new ByteArrayInputStream(bos.toByteArray())
        },
        mediaType: MT.APPLICATION_ATOM_XML
    ]
    return collection
}

def makeHref(baseUri, uri, ext) {
    return uri.replace(baseUri, "").replace('#', '')+"/"+ext
}

//======================================================================

main()

