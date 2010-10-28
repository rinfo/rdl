#!/usr/bin/env groovy
/*
 * Converts raw data.riksdagen.se download into a Depot.
 *
 * IMPORTANT: Eats memory; prepare by:
 *
 *  $ export JAVA_OPTS="-Xms512Mb -Xmx1024Mb"
 *
 */

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
        eachSourceTimeline(sourceDir, '.html') { fileMap, timestamp, docId ->
            println "Converting files for <${fileMap.html}> (${docId})..."
            def repo
            def gotFromList = false
            try {
                repo = toRDFRepo(fileMap.xml)
            } catch (Exception e1) {
                System.err.println "Error 1 in conversion to RDF: ${e1}"
                System.err.println "Trying index with doc id ${docId}..."
                try {
                    // NOTE: reparsing list xml for every doc is fast enough.
                    repo = toRDFRepo(new File(fileMap.html.parentFile, "index.xml"), docId)
                    gotFromList = true
                } catch (Exception e2) {
                    System.err.println "Error 2 in conversion to RDF: ${e2}"
                    return
                }
                // TODO: extract rpubl:utgarFran using: /(SOU \d+:[^.]+?)\)?\s*\.{4,}/
            }
            try {
                def describer = new Describer(repo.connection).
                        setPrefix('rpubl', "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#")
                def uri = ["rpubl:Proposition", "rpubl:Utredningsbetankande"].collect(
                        describer.&getByType).find { it }[0].about
                describer.close()
                def inStream = RDFUtil.toInputStream(repo, MediaType.RDF_XML, true)
                def sources = [new SourceContent(inStream, MediaType.RDF_XML)]
                if (fileMap.html) {
                    sources << new SourceContent(fileMap.html, MediaType.HTML, LANG)
                }
                println "Storing in depot as <${uri}>..."
                // TODO: remove this when depot can handlewith lots of same tstamps?
                // .. and instead use original 'systemdatum'.
                timestamp = new Date()
                if (depot.hasEntry(new URI(uri))) {
                    println "Found entry with same URI! Skipping new entry..."
                    // TODO: if (gotFromList) update?
                } else {
                    session.createEntry(new URI(uri), timestamp, sources)
                }
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
            def fnmatch = (it.name =~ /^(.+?)-(\w+?)(:\w+)?\.\w+?$/)
            if (!fnmatch) {
                throw new RuntimeException("Unexpected file: <${it}>")
            }
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


scriptFile = new File(this.class.protectionDomain.codeSource.location.toURI())
tFactory = TransformerFactory.newInstance()
transformer = tFactory.newTransformer(new StreamSource(
            new File(scriptFile.parent, "dokument-rdf.xslt")))

def toRDFRepo(File file, String docId=null) {
    def outs = new ByteArrayOutputStream()
    try {
        if (docId) {
            transformer.setParameter("id-in-list", docId)
        }
        println "Transforming <${file.path}> to RDF..."
        transformer.transform(new StreamSource(file), new StreamResult(outs))
    } finally {
        outs.close()
    }
    def repo = RDFUtil.createMemoryRepository()
    def ins = new ByteArrayInputStream(outs.toByteArray())
    try {
        RDFUtil.loadDataFromStream(repo, ins, "", MediaType.RDF_XML)
    } catch (Exception e) {
        System.err.println "Error in RDF from <${file.path}>:"
        System.err.println new String(outs.toByteArray(), "utf-8")
        throw e
    } finally {
        ins.close()
    }
    def conn = repo.connection
    try {
        if (conn.empty) throw new RuntimeException("Empty RDF.")
    } finally {
        conn.close()
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


if (args.length != 2) {
    println "Usage: SOURCE_DIR DEPOT_DIR"
} else {
    createDepotFromDataDump(new File(args[0]), new File(args[1]))
}

