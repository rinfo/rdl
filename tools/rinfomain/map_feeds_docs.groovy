
import se.lagrummet.rinfo.store.depot.DefaultPathHandler
import groovy.xml.StreamingMarkupBuilder


pathHandler = new DefaultPathHandler()
rinfoPublBase = "http://rinfo.lagrummet.se/publ/"

@Grab(group='se.lagrummet.rinfo', module='rinfo-store', version='1.0-SNAPSHOT')
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
    path = matcher.replaceFirst("bil${ord}${matcher[0][1]}")
    if (!new File(path).exists()) {
        path = matcher.replaceFirst("AR${matcher[0][1]}")
    }
    return path
}


def ant = new AntBuilder()

def slurper = new XmlSlurper()
def markupBuilder = new StreamingMarkupBuilder()

docsBase = "../../documentation/exempel/documents/publ"
feedBase = "../../documentation/exempel/feeds"

def examplePaths = ant.fileScanner {
        fileset(dir:"${docsBase}", includes:"**/*.*")
    }.findAll{ !it.hidden }.collect { it.canonicalPath }

ant.fileScanner {
    fileset(dir:"${feedBase}", includes:"**/*.atom")
}.each {
    def feed = slurper.parse(it)
    feed.declareNamespace('a':"http://www.w3.org/2005/Atom")

    // TODO: use feedDirName and logicalPath:s to create collectable feeds of it all.
    def feedDirName = (feed.id =~ /tag:([^,]+),/)[0][1]
    println "Feed <${it.name}>:"

    feed.'a:entry'.each {
        def id = it.'a:id'
        //println "Entry <${it.'a:id'}>:"
        it.'a:content'.each {
            def logicalPath = contentFilePath("${id}", "${it.@src}", "${it.@type}")
            def fpath = new File(docsBase + logicalPath).canonicalPath
            if (!examplePaths.remove(new File(fpath).canonicalPath)) {
                println "[NotFound]:"
                println "<${fpath}> = <${it.@src}>"
            }
            def newPath = feedDirName + logicalPath
            println "Copy file to ${newPath} (was ${it.@src})"
            it.@src = "/"+newPath
        }
        def enclCount = 1
        it.'a:link'.each {
            def logicalPath = (it.@rel == "alternate")?
                    contentFilePath("${id}", "${it.@href}", "${it.@type}") :
                    enclosureFilePath("${id}", "${it.@href}", "${it.@type}", enclCount++)
            def fpath = new File(docsBase + logicalPath).canonicalPath
            if (!examplePaths.remove(new File(fpath).canonicalPath)) {
                println "[NotFound]:"
                println "<${fpath}> = <${it.@href}>"
            }
            def newPath = feedDirName + logicalPath
            println "Copy file to ${newPath} (was ${it.@href})"
            it.@href = "/"+newPath
        }
        //println()
    }
    def feedPath = feedDirName+"/current.atom"
    feed.'a:link'.each {
        if (it.@rel == "self")
            it.@href = "/"+feedPath
    }
    def result = markupBuilder.bind{ mkp.yield feed }
    println result
    println "TODO: mkdir ${feedDirName}; write(result, ${feedPath})"
}

if (examplePaths) {
    println "Example files not present in feeds:"
    examplePaths.each { println it }
}

