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
import org.restlet.data.MediaType
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
        createAtomCollection(FEED_URI, FEED_TITLE, BASE_URI,
            collectItems(base) ) )
}

//======================================================================

def collectItems(base) {
    def items = []

    FU.iterateFiles(new File(base, "model"),
            ["n3"] as String[], true).each {
        items << modelItem(it)
    }

    items << datasetItem("ext/modeldata",
            FU.listFiles(new File(base, "extended/rdf"),
                ["n3"] as String[], true))

    items << datasetItem("serie",
            FU.listFiles(new File(base, "datasets/serie"),
                ["n3"] as String[], true))

    return items
}

def modelItem(File file) {
    def repo = RDFUtil.createMemoryRepository()
    def conn = repo.connection
    RDFUtil.loadDataFromFile(repo, file)
    def modelUri = null
    RDFUtil.one(conn, null, RDF.TYPE, OWL.ONTOLOGY, true).each {
        modelUri = it.subject as String
    }
    conn.close()
    return [
        uri: modelUri,
        updated: new Date(file.lastModified()),
        content: [data: { repoToInStream(repo) }, mediaType: MediaType.APPLICATION_RDF_XML],
        enclosures: null
    ]
}

def datasetItem(uriPath, List<File> files) {
    def itemUri = BASE_URI+"/"+uriPath
    def enclosures = []
    def youngestEnclDate = null

    def setRepo = RDFUtil.createMemoryRepository()
    def conn = setRepo.connection
    for (file in files) {
        def slug = "/"+uriPath+"/"+file.name.replace(".n3", ".rdf")

        conn.add(
                setRepo.valueFactory.createURI(itemUri),
                RDFS.SEEALSO,
                setRepo.valueFactory.createURI(BASE_URI + slug))

        enclosures << [
            href: slug,
            data: {
                def enclRepo = RDFUtil.createMemoryRepository()
                RDFUtil.loadDataFromFile(enclRepo, file)
                repoToInStream(enclRepo)
            },
            mediaType: MediaType.APPLICATION_RDF_XML
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
        content: [data: { repoToInStream(setRepo) }, mediaType: MediaType.APPLICATION_RDF_XML],
        enclosures: enclosures
    ]
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

    for (item in items) {
        def entry = Abdera.instance.newEntry()
        entry.id = item.uri
        entry.setTitle(item.uri)
        entry.setUpdated(item.updated)
        def contentHref = makeHref(baseUri, item.uri, "rdf")
        entry.setContent(new IRI(contentHref), item.content.mediaType as String)
        collection[contentHref] = item.content

        for (encl in item.enclosures) {
            entry.addLink(encl.href, "enclosure", encl.mediaType as String,
                    null/*title*/, null/*lang*/, -1)
            collection[encl.href] = encl
        }
        feed.insertEntry(entry)
    }

    collection["/"] = [
        data: {
            def bos = new ByteArrayOutputStream()
            feed.writeTo(bos)
            bos.close()
            return new ByteArrayInputStream(bos.toByteArray())
        },
        mediaType: MediaType.APPLICATION_ATOM_XML
    ]
    return collection
}

def makeHref(baseUri, uri, ext) {
    return uri.replace(baseUri, "").replace('#', '')+"/"+ext
}

//======================================================================

class BaseRestlet extends Restlet {
    def collection
    BaseRestlet(collection) {
        this.collection = collection
    }
    void handle(Request request, Response response) {
        def path = request.resourceRef.relativeRef.path
        def repr = collection[path as String]
        if (repr) {
            response.setEntity(
                new InputRepresentation(repr.data(), repr.mediaType) )
        }
    }
}

def startServer(port, collection) {
    def restlet = new BaseRestlet(collection)
    new Server(Protocol.HTTP, port, restlet).start()
}

//======================================================================

main()

