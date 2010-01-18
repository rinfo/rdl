import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import se.lagrummet.rinfo.store.depot.DefaultPathHandler
import groovy.xml.StreamingMarkupBuilder


@Grab(group='se.lagrummet.rinfo', module='rinfo-store', version='1.0-SNAPSHOT')
def createServableSources(buildDir, docsBase, feedBase) {

    def ant = new AntBuilder()

    exampleFilePaths = ant.fileScanner {
            fileset(dir:"${docsBase}", includes:"**/*.*")
        }.findAll{ !it.hidden }.collect { it.canonicalPath }

    processEntryPath = { feedDirName, examplePath, logicalPath ->
        def fpath = new File(docsBase + logicalPath).canonicalPath
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

        //println "Feed <${it.name}>:"
        def feedDirName = (feed.id =~ /tag:([^,]+),/)[0][1]
        def feedPath = feedDirName+"/current.atom"
        feedPaths << feedPath

        feed.links.each {
            if (it.rel == "self")
                it.href = "/"+feedPath
            if (it.rel =~ /.+-archive/)
                it.discard()
        }
        feed.entries.each {
            def id = it.id as String
            //println "Entry <${it.'a:id'}>:"
            it.contentElement.with {
                def logicalPath = contentFilePath(id, "${it.src}", "${it.mimeType}")
                it.src = processEntryPath(feedDirName, it.src, logicalPath)
            }
            def enclCount = 1
            it.links.each {
                def logicalPath = (it.rel == "alternate")?
                        contentFilePath(id, "${it.href}", "${it.mimeType}") :
                        enclosureFilePath(id, "${it.href}", "${it.mimeType}", enclCount++)
                it.href = processEntryPath(feedDirName, it.href, logicalPath)
            }
            //println()
        }
        def newFeedFile = new File(buildDir+'/'+feedPath)
        ant.mkdir(dir:newFeedFile.parentFile)
        newFeedFile.withWriter feed.&writeTo
    }

    def f = new File(buildDir+"/system/sources.rdf")
    ant.mkdir(dir:f.parentFile)
    def writer = new FileWriter(f)
    makeSourcesRdf(writer, buildDir, feedPaths)
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
        case ~/.+fs$/:
            return [coll.toUpperCase(), "Forfattningar/${coll.toUpperCase()}"]
        default:
            throw new RuntimeException("No category for: ${collection}")
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

def makeSourcesRdf(writer, buildDir, feedPaths) {
    def mb = new groovy.xml.MarkupBuilder(writer)
    mb.'rdf:RDF'('xmlns:rdf':"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            'xmlns:dct':"http://purl.org/dc/terms/",
            'xmlns:void':"http://rdfs.org/ns/void#",
            'xmlns:iana':"http://www.iana.org/assignments/relation/") {
        'void:Dataset'('rdf:about':"tag:lagrummet.se,2009:rinfo") {
            feedPaths.each { feedPath ->
                'dct:source' {
                    def feedBase = (feedPath =~ /(.+)\/.*/)[0][1]
                    'void:Dataset'('rdf:about':"tag:${feedBase},2009:rinfo") {
                        'sioc:feed'('rdf:resource':"/${feedPath}")
                    }
                }
            }
        }
    }
}


//========================================

def buildDir = args[0]
def docsDir = args[1]
createServableSources(buildDir,
        "${docsDir}/exempel/documents/publ", "${docsDir}/exempel/feeds")

