
import se.lagrummet.rinfo.store.depot.DefaultPathHandler

pathHandler = new DefaultPathHandler()
rinfoPublBase = "http://rinfo.lagrummet.se/publ/"
exampleBase = "documents/publ"

@Grab(group='se.lagrummet.rinfo', module='rinfo-store', version='1.0-SNAPSHOT')
def contentFilePath(id, href, mediaType) {
    def docPath = id.replace(rinfoPublBase, '').split('/')
    def docLeafParts = docPath[-1].split(':')
    def (collection, collectionPath) = collectionAndPathFor(docPath[-2])
    def year = docLeafParts[0]
    def docName = collection+'-'+docLeafParts.join('_')
    def suffix = pathHandler.hintForMediaType(mediaType)
    return exampleBase + "/${collectionPath}/${year}/${docName}.${suffix}"
}

def collectionAndPathFor(coll) {
    switch (coll) {
        case 'dir':
            return [coll, "Direktiv"]
        case 'ds':
            return ["Ds", "Forarbeten/Ds"]
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

def examplePaths = ant.fileScanner {
        fileset(dir:".", includes:"${exampleBase}/**/*.*")
    }.findAll{ !it.hidden }.collect { it.canonicalPath }

ant.fileScanner {
    fileset(dir:".", includes:"feeds/**/*.atom")
}.each {
    println "Feed <${it.name}>:"
    def feed = slurper.parse(it)
    feed.declareNamespace('a':"http://www.w3.org/2005/Atom")
    feed.'a:entry'.each {
        def id = it.'a:id'
        println "Entry <${it.'a:id'}>:"
        it.'a:content'.each {
            def fpath = contentFilePath("${id}", "${it.@src}", "${it.@type}")
            if (!examplePaths.remove(new File(fpath).canonicalPath)) print "[NotFound]"
            println "<${fpath}> = <${it.@src}>"
        }
        def ord = 1
        it.'a:link'.each {
            def fpath = (it.'@rel' == "alternate")?
                    contentFilePath("${id}", "${it.@src}", "${it.@type}") :
                    enclosureFilePath("${id}", "${it.@src}", "${it.@type}", ord++)
            if (!examplePaths.remove(new File(fpath).canonicalPath)) print "[NotFound]"
            println "<${fpath}> = <${it.@href}>"
        }
        println()
    }
}

if (examplePaths) {
    println "Example files not present in feeds:"
    examplePaths.each { println it }
}

