import org.apache.commons.io.FileUtils

import org.openrdf.model.Resource
import org.openrdf.model.Statement
import org.openrdf.model.URI
import org.openrdf.model.Value
import org.openrdf.model.ValueFactory
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.sail.SailRepository
import org.openrdf.rio.RDFFormat
import org.openrdf.rio.RDFWriterRegistry
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.RDFS
import org.openrdf.model.vocabulary.OWL

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.SourceContent


depot = FileDepot.newConfigured(args[0])


/**
 *
 */
def addModel(File file) {
    def repo = RDFUtil.createMemoryRepository()
    def conn = repo.connection
    RDFUtil.loadDataFromFile(repo, file)

    def modelUri = null
    for (st in RDFUtil.one(conn, null, RDF.TYPE, OWL.ONTOLOGY, true)) {
        modelUri = new java.net.URI(st.subject.toString())
    }
    def rdfXmlType = RDFFormat.RDFXML.defaultMIMEType
    def rdfXml = RDFUtil.serializeAsInputStream(repo, rdfXmlType)
    conn.close()
    repo.shutDown()

    def fileDate = new Date(file.lastModified())
    def contents = [new SourceContent(rdfXml, rdfXmlType)]
    return addOrUpdate(modelUri, fileDate, contents)
}


/**
 *
 */
def addDataset(String entryUriPath, List<File> files) {
    // TODO: scan resources and "figure out common base"?

    def repo = RDFUtil.createMemoryRepository()
    def conn = repo.connection

    def rdfXmlType = RDFFormat.RDFXML.defaultMIMEType
    def enclosures = []
    def youngestEnclDate = null

    def entryUri = "${depot.baseUri}/${entryUriPath}"

    for (file in files) {
        def enclRepo = RDFUtil.createMemoryRepository()
        RDFUtil.loadDataFromFile(enclRepo, file)
        def enclRdfXml = RDFUtil.serializeAsInputStream(enclRepo, rdfXmlType)

        // TODO: what mechanism for pathToDepotURI?
        // FIXME: no hardcoded suffixes; subsumed paths..
        def fname = file.name.replace(".n3", ".rdf")
        def slug = "/${entryUriPath}/${fname}"

        def vf = repo.valueFactory
        conn.add(
                vf.createURI(entryUri),
                RDFS.SEEALSO,
                vf.createURI(depot.baseUri.toString() + slug))

        enclosures << new SourceContent(enclRdfXml, rdfXmlType, null, slug)

        def fileDate = new Date(file.lastModified())
        if (!youngestEnclDate || fileDate > youngestEnclDate) {
            youngestEnclDate = fileDate
        }
    }

    def rdfXml = RDFUtil.serializeAsInputStream(repo, rdfXmlType)

    conn.close()
    repo.shutDown()

    // TODO: as "subsumer" (i.e. supply captures sub-uri:s and 303:s..)?
    return addOrUpdate(
            new java.net.URI(entryUri),
            youngestEnclDate,
            [new SourceContent(rdfXml, rdfXmlType)],
            enclosures
        )

}


def addOrUpdate(uri, lastMod, contents, enclosures=null) {
    def entry = depot.getEntry(uri)
    def newDate = new Date()
    if (entry) {
        if (lastMod > entry.updated) {
            entry.update(newDate, contents, enclosures)
        } else {
            //logger.info
            print "Skipping resource with URI <${uri}>"
            println " - not modified since ${entry.updated}."
            return null
        }
    } else {
        //logger.info
        println "Storing model ${uri}"
        entry = depot.createEntry(uri, newDate, contents, enclosures)
    }
    return entry
}


def baseToDepot() {

    def rinfoBase = "../../../resources/base/"

    def batch = depot.makeEntryBatch()
    def addToBatch = {
        if (it) { batch << it }
    }

    // store models
    def baseDir = new File(rinfoBase, "model")
    FileUtils.iterateFiles(baseDir, ["n3"] as String[], true).each {
        addToBatch addModel(it)
    }

    // store the dataset "ext/modeldata"
    baseDir = new File(rinfoBase, "extended/rdf")
    addToBatch addDataset("ext/modeldata",
            FileUtils.listFiles(baseDir, ["n3"] as String[], true))

    // store the dataset "serie"
    baseDir = new File(rinfoBase, "datasets/serie")
    addToBatch addDataset("serie",
            FileUtils.listFiles(baseDir, ["n3"] as String[], true))

    depot.indexEntries(batch)

}

baseToDepot()

