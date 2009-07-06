import org.apache.commons.io.FileUtils as FU

import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI

import org.openrdf.rio.RDFFormat
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.RDFS
import org.openrdf.model.vocabulary.OWL

import se.lagrummet.rinfo.base.rdf.RDFUtil

import org.restlet.Restlet
import org.restlet.Server
import org.restlet.data.MediaType as MT
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.resource.InputRepresentation


FEED_URI = "http://rinfo-admin.lagrummet.se/base/feed"
FEED_TITLE = "RInfo Base Data"

BASE_URI = "http://rinfo.lagrummet.se"

def main() {
    def base = args ? args[0] : "../../../resources/base/"
    def port = 8280
    startServer(port,
        { createAtomCollection(FEED_URI, FEED_TITLE, BASE_URI,
            collectItems(base) ) } )
    // TODO: pingMainWithMe?
}

//======================================================================

class BaseRestlet extends Restlet {
    Closure makeCollection
    def collection
    BaseRestlet(makeCollection) {
        this.makeCollection = makeCollection
    }
    void handle(Request request, Response response) {
        def path = request.resourceRef.relativeRef.path as String
        if (collection == null || path == "/") {
            collection = makeCollection()
        }
        def repr = collection[path]
        if (repr) {
            response.setEntity(
                new InputRepresentation(repr.data(), repr.mediaType) )
        }
    }
}

def startServer(port, makeCollection) {
    def restlet = new BaseRestlet(makeCollection)
    new Server(Protocol.HTTP, port, restlet).start()
}

//======================================================================

def collectItems(base) {
    def items = []
    def addItem = {
        if (it) items << it
    }

    FU.iterateFiles(new File(base, "model"),
            ["n3"] as String[], true).each {
        addItem modelItem(it)
    }

    // Doesn't work - will use external model URI:s (collector won't load those).
    //FU.iterateFiles(new File(base, "../external/rdf"),
    //        ["rdfs", "owl"] as String[], true).each {
    //    addItem modelItem(it)
    //}
    addItem datasetItem("ext/models",
            FU.listFiles(new File(base, "../external/rdf"),
                ["rdfs", "owl"] as String[], true))

    addItem datasetItem("ext/extended_modeldata",
            FU.listFiles(new File(base, "extended/rdf"),
                ["n3"] as String[], true))

    addItem datasetItem("org",
            FU.listFiles(new File(base, "datasets/org"),
                ["n3"] as String[], true))

    addItem datasetItem("serie",
            FU.listFiles(new File(base, "datasets/serie"),
                ["n3"] as String[], true))

    addItem datasetItem("system",
            FU.listFiles(new File(base, "datasets"),
                ["n3"] as String[], false))

    return items
}

def modelItem(File file) {
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
            data: managedRdfInputStream(file, repo),
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

def datasetItem(uriPath, List<File> files) {
    def itemUri = BASE_URI+"/"+uriPath
    def enclosures = []
    def youngestEnclDate = null

    def setRepo = RDFUtil.createMemoryRepository()
    def conn = setRepo.connection
    files.each { file ->
        def slug = "/"+uriPath+"/"+file.name.replace(".n3", ".rdf")

        conn.add(
                setRepo.valueFactory.createURI(itemUri),
                RDFS.SEEALSO,
                setRepo.valueFactory.createURI(BASE_URI + slug))

        enclosures << [
            href: slug,
            data: managedRdfInputStream(file),
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
Closure managedRdfInputStream(file, final repo=null) {
    if (file.name.endsWith(".n3")) {
        if (repo == null) {
            repo = RDFUtil.createMemoryRepository()
            RDFUtil.loadDataFromFile(repo, file)
        }
        return { repoToInStream(repo) }
        repo.shutDown()
    } else {
        return { new FileInputStream(file) }
    }
}

def repoToInStream(repo) {
    return RDFUtil.serializeAsInputStream(repo, RDFFormat.RDFXML.defaultMIMEType)
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

    collection["/"] = [
        data: {
            def bos = new ByteArrayOutputStream()
            feed.writeTo(bos)
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

