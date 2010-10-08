#!/usr/bin/env groovy

@Grapes([
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
    @Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
])
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.apache.abdera.Abdera

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.SourceContent


class MediaType {
    static RDF_XML = "application/rdf+xml"
    static HTML = "text/html"
    static TEXT = "text/plain"
}

LANG = "sv"


void createDepotFromDataDump(File sourceDir, File depotDir, boolean debug=false) {
    def depot = newDepot(depotDir)
    println "Creating depot in <${depotDir}>..."
    withDepotSession(depot) { session ->
        //def currentList = [path:null, xml:null]
        eachSourceTimeline(sourceDir, '.html') { fileMap, timestamp, docId ->
            println "Converting files for <${fileMap.html}> (${docId})..."
            def repo
            try {
                repo = toRDFRepo(fileMap.xml)
                def conn = repo.connection
                try {
                    if (conn.empty) throw new RuntimeException("Empty RDF.")
                } finally {
                    conn.close()
                }
            } catch (Exception e) {
                System.err.println "Error in conversion to RDF: ${e}"
                // TODO: if xml fails or is missing: use xml from list ("id-in-list", docId)
                return
            }
            try {
                //saveToEntry(session, repo, fileMap)
                def describer = new Describer(repo.connection).
                        setPrefix('rpubl', "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#")
                def uri = describer.getByType("rpubl:Proposition")[0].about
                describer.close()
                def inStream = RDFUtil.toInputStream(repo, MediaType.RDF_XML, true)
                def sources = [new SourceContent(inStream, MediaType.RDF_XML)]
                if (fileMap.html) {
                    sources << new SourceContent(fileMap.html, MediaType.HTML, LANG)
                }
                println "Storing in depot as <${uri}>..."
                session.createEntry(new URI(uri), timestamp, sources)
            } finally {
                repo.shutDown()
            }
        }
    }
    println "Done."
}


def eachSourceTimeline(File sourceDir, String suffix, Closure handle) {
    def fileTimeIds = []
    sourceDir.eachFileRecurse(groovy.io.FileType.FILES) {
        if (it.name.endsWith(suffix) && !it.name.startsWith('index')) {
            def fnmatch = (it.name =~ /^(.+?)-(\w+?)\.\w+?$/)
            def (_, dateRepr, docId) = fnmatch[0]
            def timestamp = Date.parse("yyyy-MM-dd'T'HH'_'mm'_'ss", dateRepr)
            fileTimeIds << [it, timestamp, docId]
        }
    }
    fileTimeIds.sort { it[1] }
    for (it in fileTimeIds) {
        def (file, timestamp, docId) = it
        def commonPart = file.name.replace(suffix, '') + '.'
        def files = file.parentFile.listFiles({
                it.name.startsWith(commonPart) } as FileFilter)
        def fileMap = files.inject([:]) { map, f ->
                    map[(f.name =~ /\.([^.]+)$/)[0][1]] = f
                    return map
                }
        handle(fileMap, timestamp, docId)
    }
}


tFactory = TransformerFactory.newInstance()
transformer = tFactory.newTransformer(new StreamSource(new File("prop-rdf.xslt")))

def toRDFRepo(File file) {
    def outs = new ByteArrayOutputStream()
    try {
        transformer.transform(new StreamSource(file), new StreamResult(outs))
    } finally {
        outs.close()
    }
    def repo = RDFUtil.createMemoryRepository()
    def ins = new ByteArrayInputStream(outs.toByteArray())
    try {
        RDFUtil.loadDataFromStream(repo, ins, "", MediaType.RDF_XML)
    } catch (Exception e) {
        println "Error in RDF from <${file.path}>:"
        println new String(outs.toByteArray(), "utf-8")
        throw e
    } finally {
        ins.close()
    }
    return repo
}


Depot newDepot(File depotDir) {
    def dataset = depotDir.name
    def baseUri = new URI("http://rinfo.lagrummet.se/")
    depot = new FileDepot(baseUri, depotDir)
    depot.with {
        atomizer.feedPath = "/feed"
        atomizer.feedBatchSize = 100
        atomizer.feedSkeleton = Abdera.instance.newFeed().with {
            id = "tag:data.diksdagen.se,2010:rinfo:data:${dataset}"
            return it
        }
    }
    return depot
}

void withDepotSession(Depot depot, Closure handle) {
    def session = depot.openSession()
    try {
        handle(session)
    } finally {
        session.close()
    }
}

try {
    createDepotFromDataDump(new File(args[0]), new File(args[1]))
} catch (IndexOutOfBoundsException e) {
    println "Usage: SOURCE_DIR DEPOT_DIR"
}

