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

import se.lagrummet.rinfo.util.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.SourceContent

import org.springframework.context.support.ClassPathXmlApplicationContext as Ctxt


context = new Ctxt("applicationContext.xml")
depot = context.getBean("fileDepot")


/**
 *
 */
def addModel(File file) {
    def format = RDFFormat.forFileName(file.name)
    def repo = RDFUtil.createMemoryRepository()
    RDFUtil.loadDataFromFile(repo, file)

    def modelUri = null

    for (st in RDFUtil.one(repo, null, RDF.TYPE, OWL.ONTOLOGY, true)) {
        modelUri = new java.net.URI(st.subject.toString())
    }

    def rdfXmlType = RDFFormat.RDFXML.defaultMIMEType
    def rdfXml = RDFUtil.serializeAsInputStream(repo, rdfXmlType)

    def entry = depot.getEntry(modelUri)
    def fileDate = new Date(file.lastModified())
    if (entry) {
        // TODO: check updated vs file timestamp
        if (fileDate > entry.updated) {
            // ...
        } else {
            //logger.info
            print "Skipping model with URI <${modelUri}>"
            println " - not modified since ${entry.updated}."
        }
    } else {
        //logger.info
        println "Storing model ${modelUri}"
        depot.createEntry(modelUri, fileDate,
                [new SourceContent(rdfXml, rdfXmlType)])
    }
    repo.shutDown()
}


/**
 *
 */
def addDataset(String entryUriPath, List<File> files) {
    // TODO: scan resources and "figure out common base"?

    def repo = RDFUtil.createMemoryRepository()

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
        repo.connection.add(
                vf.createURI(entryUri),
                RDFS.SEEALSO,
                vf.createURI(depot.baseUri.toString() + slug))

        enclosures << new SourceContent(enclRdfXml, rdfXmlType, null, slug)

        def fileDate = new Date(file.lastModified())
        if (!youngestEnclDate || youngestEnclDate > fileDate) {
            youngestEnclDate = fileDate
        }
    }

    def rdfXml = RDFUtil.serializeAsInputStream(repo, rdfXmlType)

    // TODO: as "subsumer" (i.e. supply captures sub-uri:s and 303:s..)?
    depot.createEntry(
            new java.net.URI(entryUri),
            youngestEnclDate,
            [new SourceContent(rdfXml, rdfXmlType)], enclosures)

}



def baseToDepot() {

    def rinfoBase = "../../resources/base/"

    // store models
    def baseDir = new File(rinfoBase, "model")
    FileUtils.iterateFiles(baseDir, ["n3"] as String[], true).each {
        addModel(it)
    }

    // store the dataset "serie"
    def serieFiles = []
    def baseUri = "ref"
    baseDir = new File(rinfoBase, "datasets/serie")
    FileUtils.iterateFiles(baseDir, ["n3"] as String[], true).each {
        serieFiles << it
    }
    addDataset(baseUri, serieFiles)

}

baseToDepot()

