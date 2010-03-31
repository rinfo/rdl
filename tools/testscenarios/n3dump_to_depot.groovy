/*
Works with sources from <http://trac.lagen.nu/wiki/DataSets>:
    - https://lagen.nu/sfs/parsed/rdf.nt
    - https://lagen.nu/dv/parsed/rdf.nt
Eats memory; prepare by:
    export JAVA_OPTS="-Xms512Mb -Xmx1024Mb"
*/

@Grapes([
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
    @Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
])
import static org.apache.commons.lang.StringUtils.replaceOnce
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.XMLSchema
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.*


RINFO = "http://rinfo.lagrummet.se/"
RPUBL  = "${RINFO}ns/2008/11/rinfo/publ#"
LEGACY_PUBL = "${RINFO}taxo/2007/09/rinfo/pub#"
LAGENNNU = "http://lagen.nu/terms#"
DCT = "http://purl.org/dc/terms/"

GRUNDLAG_NUMBERS = [
    '1974:152', // regeringsform
    '1810:0926', // successionsordningen
    '1949:105', // tryckfrihetsförordningen
    '1991:1469', // yttrandefrihetsgrundlagen
]


def convertLagenNuNTLines(URI uri, List<String> lines) {
    def newLines = rewriteLagenNuNTLines(lines)

    def typeTripleIndex = newLines.findIndexOf { it.startsWith("<${uri}> <${RDF.TYPE}> ") }

    // retype:
    if (GRUNDLAG_NUMBERS.find(uri.toString().&endsWith)) {
        newLines[typeTripleIndex] = "<${uri}> <${RDF.TYPE}> <${RPUBL}Grundlag> ."

    } else if (isLaw(newLines)) {
        newLines[typeTripleIndex] = "<${uri}> <${RDF.TYPE}> <${RPUBL}Lag> ."

    } else if (typeTripleIndex == -1) {
        newLines += [
            "<${uri}> <${RDF.TYPE}> <${RPUBL}Forordning> .",
            "<${uri}> <${RPUBL}forfattningssamling> <${RINFO}serie/fs/sfs> .",
            "<${uri}> <http://purl.org/dc/terms/publisher> <${RINFO}org/regeringskansliet> .",
        ]
    }

    // only for type:
    if (newLines[typeTripleIndex].endsWith("<${RPUBL}Rattsfallsreferat> .")) {
        newLines = newLines.collect {
            def s = replaceOnce(it, "<${DCT}description>", "<${RPUBL}referatrubrik>")
            // TODO: is "issued" good enough?
            s = replaceOnce(s, "<${RPUBL}avgorandedatum>", "<${DCT}issued>")
            s = s.replaceAll(/(.+) <${DCT}identifier> "\w+ ([^"]+)"@sv \./,
                            '$1 <'+RPUBL+'publikationsplatsangivelse> "$2" .')
            return s
        }
    }

    return newLines
}

def rewriteLagenNuNTLines(lines) {
    return lines.findAll {
        !( it.find("<${LAGENNNU}senastHamtad>") ||
            it.find("<${LEGACY_PUBL}konsoliderar>") ||
            it.find("<${LEGACY_PUBL}konsolideringsunderlag>") ||
            it.find('relation> ""')
        )
    }.collect {
        // TODO:? are org refs always correct?
        def s = replaceOnce(it, "<http://lagen.nu/org/2008/", "<${RINFO}org/")

        s = replaceOnce(s, "<${RINFO}ref/sfs>", "<${RINFO}serie/fs/sfs>")
        s = replaceOnce(s, "<${RINFO}ref/fs", "<${RINFO}serie/fs")
        s = replaceOnce(s, "<${RINFO}ref/rff", "<${RINFO}serie/rff")

        s = replaceOnce(s, "<${LAGENNNU}paragrafnummer>", "<${RPUBL}paragrafnummer>")
        s = replaceOnce(s, "<${LEGACY_PUBL}forfattningsamling>", "<${RPUBL}forfattningssamling>")
        s = replaceOnce(s,
                "<${LEGACY_PUBL}KonsolideradGrundforfattning>", "<${RPUBL}Forordning>")
        s = replaceOnce(s, "<${LEGACY_PUBL}", "<${RPUBL}")
        s = s.replaceAll(/("\d{4}-\d{2}-\d{2}")(@\w+)?/, '$1'+"^^<${XMLSchema.NAMESPACE}date>")

        s = s.replaceAll(/#K(\d+)P/, '#k_$1-p_')
        s = replaceOnce(s, "#K", "#k_")
        s = replaceOnce(s, "#P", "#p_")

        s = replaceOnce(s, "<${RINFO}publ/rattsfall/", "<${RINFO}publ/rf/")
        return s
    }
}

def fixUri(uri) {
    return replaceOnce(uri, "${RINFO}publ/rattsfall/", "${RINFO}publ/rf/")
}

def isLaw(lines) {
    def titleTriple = lines.find { it =~ "title>" }
    if (!titleTriple)
        return false
    def title = titleTriple.replaceFirst(/.+"([^"]+)"@sv\s*.$/, '$1')
    return (title.startsWith('Lag ') ||
            (title.endsWith('lag') && !title.startsWith('Förordning')) ||
            title.endsWith('balk'))
}


RDFXML_MEDIA_TYPE = "application/rdf+xml"

def createEntry(session, uri, nt) {
    def repo = RDFUtil.createMemoryRepository()
    RDFUtil.loadDataFromStream(repo,
            new ByteArrayInputStream(nt.getBytes("utf-8")),
            "", "text/plain")
    def inStream = RDFUtil.toInputStream(repo, RDFXML_MEDIA_TYPE, true)
    try {
        def rdfContent = new SourceContent(inStream, RDFXML_MEDIA_TYPE)
        println "Creating entry <$uri>"
        session.createEntry(uri, new Date(), [rdfContent])
    } finally {
        inStream.close()
    }
}

def rdfSource
def depotDir
try {
    rdfSource = args[0]
    depotDir = new File(args[1])
} catch (IndexOutOfBoundsException e) {
    println "Usage: <rdf-source> <depot-dir>"
    System.exit 0
}

def baseUri = new URI(RINFO)
depot = new FileDepot(baseUri, depotDir)
depot.atomizer.feedPath = "/feed"
depot.atomizer.feedBatchSize = 100
//depot.atomizer.feedSkeletonPath = "feed_skeleton.atom"

def allLines = new File(rdfSource).readLines()
allLines.sort()
def grouped = allLines.groupBy {
    def m = (it =~ /^<([^>#]+)(#[^>]+)?>/);
    if (m[0]) m[0][1]
}

def session = depot.openSession()
try {
    grouped.each { key, lines ->
        def uri = new URI(fixUri(key))
        def newNtLines = convertLagenNuNTLines(uri, lines)
        def ntStr = newNtLines.join("\n")
        if (false) {
            println "DEBUG for <${uri}>:"; println ntStr; System.exit 0
        } else {
            createEntry(session, uri, ntStr)
        }
    }
} finally {
    session.close()
}

