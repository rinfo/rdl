@Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
@Grab('org.restlet.jse:org.restlet:2.0.1')
@Grab('org.restlet.jee:org.restlet.ext.servlet:2.0.1')
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Link
import se.lagrummet.rinfo.store.depot.Atomizer
import se.lagrummet.rinfo.store.depot.DefaultPathHandler
import groovy.xml.StreamingMarkupBuilder
import org.apache.commons.io.FileUtils
import org.apache.commons.codec.digest.DigestUtils

/*
 * This script processes example data, as can be seen in the documentation directory, 
 * into a format that can be served for consumption by the RInfo Main application.
 */
def createServableSources(buildDir, publicServer, docsBase, feedBase) {

    def ant = new AntBuilder()

    exampleFilePaths = ant.fileScanner {
            fileset(dir:"${docsBase}", includes:"**/*.*")
        }.findAll{ !it.hidden }.collect { it.canonicalPath }

    checkedEntryPath = { atomElt, examplePath, feedDirName, logicalPath ->
        def file = new File(docsBase + logicalPath)
        def fpath = file.canonicalPath
        if (atomElt instanceof Link && atomElt.length != -1) {
            if (atomElt.length != file.length()) {
                println "[Bad length for <${fpath}>] expected ${atomElt.length} != ${file.length()}"
            }
        }
        def expectedMd5 = atomElt.getAttributeValue(Atomizer.LINK_EXT_MD5)
        if (expectedMd5) {
            def realMd5 = DigestUtils.md5Hex(FileUtils.readFileToByteArray(file))
            if (expectedMd5 != realMd5) {
                println "[Bad MD5 for <${fpath}>] expected ${expectedMd5} != ${realMd5}"
            }
        }
        if (!exampleFilePaths.remove(fpath)) {
            println "[NotFound]:"
            println "<${fpath}> = <${examplePath}>"
        }
        def newPath = feedDirName + logicalPath
        //println "Copy file to ${newPath} (was ${examplePath})"
        ant.copy(file:fpath, tofile:buildDir+'/'+newPath)
        return "/"+newPath
    }

    ant.mkdir(dir:buildDir)

    def feedPaths = []

    ant.fileScanner { fileset(dir:"${feedBase}", includes:"**/*.atom") }.each {

        def feed = (Feed) Abdera.instance.parser.parse(new FileReader(it)).root
        if (feed.baseUri) {
            feed.baseUri = ""
        }

        println "Feed <${it.name}>:"
        def feedDirName = (feed.id =~ /tag:([^,]+),/)[0][1]
        def feedPath = feedDirName+"/current.atom"

        feed.links.each {
            if (it.rel == "self")
                it.href = "/"+feedPath
            if (it.rel =~ /.+-archive/)
                it.discard()
        }
        try {
            feed.entries.each {
                def id = it.id as String
                println "Entry <${id}>:"
                it.contentElement.with {
                    def logicalPath = contentFilePath(id, "${it.src}", "${it.mimeType}")
                    it.src = checkedEntryPath(it, it.src, feedDirName, logicalPath)
                }
                def enclCount = 1
                it.links.each {
                    def logicalPath = (it.rel == "alternate")?
                            contentFilePath(id, "${it.href}", "${it.mimeType}") :
                            enclosureFilePath(id, "${it.href}", "${it.mimeType}", enclCount++)
                    it.href = checkedEntryPath(it, it.href, feedDirName, logicalPath)
                }
                println()
            }
        } catch (Exception e) {
            System.err.println "Caught ${e}; skipping."
            return
        }
        def newFeedFile = new File(buildDir+'/'+feedPath)
        ant.mkdir(dir:newFeedFile.parentFile)
        newFeedFile.withWriter feed.&writeTo
        feedPaths << feedPath
    }

    def f = new File(buildDir+"/sys/sources.rdf")
    ant.mkdir(dir:f.parentFile)
    def writer = new FileWriter(f)
    makeSourcesRdf(writer, buildDir, feedPaths, publicServer)
    writer.close()

    if (exampleFilePaths) {
        println "Example files not present in feeds:"
        exampleFilePaths.each { println it }
    }
}


rinfoPublBase = "http://rinfo.lagrummet.se/publ/"

pathHandler = new DefaultPathHandler()

def contentFilePath(id, href, mediaType) {
    def docPath = id.replace(rinfoPublBase, '').split('/')
    def docLeafParts = docPath[-1].split(':')
    def (collection, collectionPath) = collectionAndPathFor(docPath[-2])
    def year = docLeafParts[0]
    def docName = collection+'-'+docLeafParts.join('_')
    def suffix = pathHandler.hintForMediaType(mediaType)
    return "/${collectionPath}/${year}/${docName}.${suffix}"
}

def collectionAndPathFor(coll) {
    switch (coll) {
        case 'dir':
            return [coll, "Direktiv"]
        case 'ds':
            return ["Ds", "Forarbeten/Ds"]
        case 'vervafs':
            return ["VervaFS", "Forfattningar/VervaFS"]
        case 'nja':
            return ["NJA", "Referat/NJA"]
        // TODO:
        //case 'konsolidering':
        //    return ["KonsolideradeForfattningar"]
        case ~/.+fs$/:
            return [coll.toUpperCase(), "Forfattningar/${coll.toUpperCase()}"]
        default:
            throw new RuntimeException("No category for: ${coll}")
    }
}

def enclosureFilePath(id, href, mediaType, ord) {
    def path = contentFilePath(id, href, mediaType)
    def matcher = (path =~ /(\.\w+)$/)
    if (href =~ /AR/) {
        path = matcher.replaceFirst("AR${matcher[0][1]}")
    } else {
        path = matcher.replaceFirst("bil${ord}${matcher[0][1]}")
    }
    return path
}

def makeSourcesRdf(writer, buildDir, feedPaths, publicServer) {
    def mb = new groovy.xml.MarkupBuilder(writer)
    mb.'rdf:RDF'('xmlns:rdf':"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            'xmlns:dct':"http://purl.org/dc/terms/",
            'xmlns:void':"http://rdfs.org/ns/void#",
            'xmlns:iana':"http://www.iana.org/assignments/relation/") {
        'void:Dataset'('rdf:about':"tag:lagrummet.se,2009:rinfo") {
            feedPaths.each { feedPath ->
                'dct:source' {
                    def feedBase = (feedPath =~ /(.+)\/.*/)[0][1]
                    def orgSlug = feedBase.replace('.se', '')
                    def orgUri = "http://rinfo.lagrummet.se/org/${orgSlug}"
                    'void:Dataset'('rdf:about':"tag:${feedBase},2009:rinfo") {
                        'dct:publisher'('rdf:resource':orgUri)
                        'iana:current'('rdf:resource':"${publicServer}/${feedPath}")
                    }
                }
            }
        }
    }
}


//========================================

if (args.length < 2) {
    println "Usage: groovy <script> <documentation-dir> <build-dir>"
    System.exit 0
}
def docsDir = args[0]
def buildDir = args[1]
def publicServer = args[2]
createServableSources(buildDir, publicServer,
        "${docsDir}/exempel/documents/publ", "${docsDir}/exempel/feeds")

