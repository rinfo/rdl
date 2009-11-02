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

    ant.fileScanner { fileset(dir:"${feedBase}", includes:"**/*.atom") }.each {

        def slurper = new XmlSlurper()
        def feed = slurper.parse(it)
        feed.declareNamespace('a':"http://www.w3.org/2005/Atom")

        //println "Feed <${it.name}>:"
        def feedDirName = (feed.id =~ /tag:([^,]+),/)[0][1]
        def feedPath = feedDirName+"/current.atom"
        feed.'a:link'.each {
            if (it.@rel == "self")
                it.@href = "/"+feedPath
        }
        feed.'a:entry'.each {
            def id = it.'a:id' as String
            //println "Entry <${it.'a:id'}>:"
            it.'a:content'.each {
                def logicalPath = contentFilePath(id, "${it.@src}", "${it.@type}")
                it.@src = processEntryPath(feedDirName, it.@src, logicalPath)
            }
            def enclCount = 1
            it.'a:link'.each {
                def logicalPath = (it.@rel == "alternate")?
                        contentFilePath(id, "${it.@href}", "${it.@type}") :
                        enclosureFilePath(id, "${it.@href}", "${it.@type}", enclCount++)
                it.@href = processEntryPath(feedDirName, it.@href, logicalPath)
            }
            //println()
        }
        def markupBuilder = new StreamingMarkupBuilder()
        def result = markupBuilder.bind{ mkp.yield feed }
        def newFeedFile = new File(buildDir+'/'+feedPath)
        ant.mkdir(dir:newFeedFile.parentFile)
        newFeedFile.write(result.toString())
    }

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

//========================================

createServableSources(
        "_build/www",
        "../../documentation/exempel/documents/publ",
        "../../documentation/exempel/feeds")

