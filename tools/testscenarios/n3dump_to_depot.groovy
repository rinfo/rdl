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

RPUBL  = "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
LEGACY_PUBL = "http://rinfo.lagrummet.se/taxo/2007/09/rinfo/pub#"
LAGENNNU = "http://lagen.nu/terms#"

def rewriteLagenNuNTLines(lines) {
    return lines.findAll {
        !( it.find("<${LAGENNNU}senastHamtad>") ||
            it.find("<${LEGACY_PUBL}konsoliderar>") ||
            it.find("<${LEGACY_PUBL}konsolideringsunderlag>") )
    }.collect {
        // TODO: correct org refs?
        def s = replaceOnce(it, "<http://lagen.nu/org/2008/", "<http://rinfo.lagrummet.se/org/")
        s = replaceOnce(s, "<${LAGENNNU}paragrafnummer>", "<${RPUBL}paragrafnummer>")
        s = replaceOnce(s,
                "<${LEGACY_PUBL}KonsolideradGrundforfattning>", "<${RPUBL}Forordning>")
        s = replaceOnce(s, "<${LEGACY_PUBL}", "<${RPUBL}")
        s = s.replaceAll(/("\d{4}-\d{2}-\d{2}")(@\w+)?/, '$1'+"^^<${XMLSchema.NAMESPACE}date>")
        return s
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

def baseUri = new URI("http://rinfo.lagrummet.se/")
depot = new FileDepot(baseUri, depotDir)
depot.atomizer.feedPath = "/feed"
//depot.atomizer.feedSkeletonPath = "feed_skeleton.atom"

def lines = new File(rdfSource).readLines()
lines.sort()
def grouped = lines.groupBy {
    def m = (it =~ /^<([^>#]+)(#[^>]+)?>/);
    if (m[0]) m[0][1]
}

def session = depot.openSession()
try {
    grouped.each { k, v ->
        def uri = new URI(k)
        def newNtLines = rewriteLagenNuNTLines(v)
        if (!newNtLines.find { it.contains(" <${RDF.TYPE}> ") }) {
            newNtLines += [
                "<${uri}> <${RDF.TYPE}> <${RPUBL}Forordning> .",
                "<${uri}> <${RPUBL}forfattningsamling> <http://rinfo.lagrummet.se/ref/sfs> .",
                "<${uri}> <http://purl.org/dc/terms/publisher> <http://rinfo.lagrummet.se/org/regeringskansliet> .",
            ]
        }
        createEntry(session, uri, newNtLines.join("\n"))
    }
} finally {
    session.close()
}

