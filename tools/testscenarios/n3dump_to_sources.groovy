/*
Works with sources from <http://trac.lagen.nu/wiki/DataSets>:
    - "https://lagen.nu/sfs/parsed/rdf.nt"
    - "https://lagen.nu/dv/parsed/rdf.nt"
Eats memory; prepare by:
    export JAVA_OPTS="-Xms512Mb -Xmx1024Mb"
*/

@Grapes([
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
    @Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
])
import static org.apache.commons.lang.StringUtils.replaceOnce
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

RINFO_PUBL  = "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
LAGENNNU = "http://lagen.nu/terms#"

def rewriteLagenNuNTLines(lines) {
    return lines.findAll {
        !it.find("<${LAGENNNU}senastHamtad>")
    }.collect {
        // TODO: correct org refs?
        def s = replaceOnce(it, "<http://lagen.nu/org/2008/", "<http://rinfo.lagrummet.se/org/")
        s = replaceOnce(s, "<${LAGENNNU}paragrafnummer", "<${RINFO_PUBL}paragrafnummer")
        s = replaceOnce(s,
                "<http://rinfo.lagrummet.se/taxo/2007/09/rinfo/pub#KonsolideradGrundforfattning>",
                "<${RINFO_PUBL}Forordning>")
        s = replaceOnce(s, "<http://rinfo.lagrummet.se/taxo/2007/09/rinfo/pub#", "<${RINFO_PUBL}")
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
        def nt = rewriteLagenNuNTLines(v).join("\n")
        createEntry(session, uri, nt)
    }

} finally {
    session.close()
}

