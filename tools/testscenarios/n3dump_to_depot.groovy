/**
 * Generate Atom Depots with RDF posts per document from nt data dumps of
 * lagen.nu scrapes.
 *
 * Works with sources from <http://trac.lagen.nu/wiki/DataSets>:
 *
 *  - <https://lagen.nu/sfs/parsed/rdf.nt>
 *  - <https://lagen.nu/dv/parsed/rdf.nt>
 *
 * When developing, extract some test data:
 *
 *  $ grep '&lt;http://rinfo.lagrummet.se/publ/sfs/1736' lagennu-sfs.nt &gt; lagennu-sfs-snippet.nt
 * And run:
 *
 *  $ groovy n3dump_to_depot.groovy lagennu-sfs-snippet.nt sfs-depot -debug
 *
 * IMPORTANT: Eats memory; prepare by:
 *
 *  $ export JAVA_OPTS="-Xms512Mb -Xmx1024Mb"
 *
 * TODO:
 * - see markers in the code below.
 * - It might be possible to leverage R2R
 *   <http://www4.wiwiss.fu-berlin.de/bizer/r2r/> for this (although it may
 *   still lack some specific needs).
 */

@Grapes([
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
    @Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
])
import static org.apache.commons.lang.StringUtils.replaceOnce
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.XMLSchema
import org.apache.abdera.Abdera
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.store.depot.*


def createDepotFromTriples(URI baseUri, File rdfSource, File depotDir, debug=false) {
    def uriTripleGroups = groupTriplesByLeadingUri(rdfSource)

    depot = new FileDepot(baseUri, depotDir)
    depot.atomizer.feedPath = "/feed"
    depot.atomizer.feedBatchSize = 100
    def feed = Abdera.instance.newFeed()
    feed.id = "tag:localhost,2010:rinfo:test:${depotDir.name}"
    depot.atomizer.feedSkeleton = feed

    def session = depot.openSession()
    try {
        uriTripleGroups.each { key, lines ->
            def results = convertLagenNuTripleGroups(key, lines)
            results.each { uri, ntStr ->
                def docRepo = ntStringtoRepo(ntStr)
                if (debug) {
                    println "# DEBUG - RDF for <${uri}>:"
                    RDFUtil.serialize(docRepo, RDFUtil.TURTLE,
                            new BufferedOutputStream(System.out) { void close() { println() } },
                            true)
                } else {
                    createEntry(session, new URI(uri), docRepo)
                }
                docRepo.shutDown()
            }
        }
    } finally {
        session.close()
    }
}

def groupTriplesByLeadingUri(triplesFile) {
    def allLines = new TreeSet()
    triplesFile.eachLine {
        if (!ignoreSourceLine(it))
            allLines << it
    }
    return allLines.groupBy {
        def m = (it =~ /^<([^>#]+)(#[^>]+)?>/);
        if (m[0]) m[0][1]
    }
}

def ntStringtoRepo(ntStr) {
    def repo = RDFUtil.createMemoryRepository()
    RDFUtil.loadDataFromStream(repo,
            new ByteArrayInputStream(ntStr.getBytes("utf-8")), "", "text/plain")
    return repo
}

def createEntry(session, uri, docRepo) {
    def RDFXML_MEDIA_TYPE = "application/rdf+xml"
    def inStream = RDFUtil.toInputStream(docRepo, RDFXML_MEDIA_TYPE, true)
    try {
        def rdfContent = new SourceContent(inStream, RDFXML_MEDIA_TYPE)
        println "Creating entry <$uri>"
        session.createEntry(uri, new Date(), [rdfContent])
    } finally {
        inStream.close()
    }
}


RINFO = "http://rinfo.lagrummet.se/"
RPUBL  = "${RINFO}ns/2008/11/rinfo/publ#"
LEGACY_PUBL = "${RINFO}taxo/2007/09/rinfo/pub#"
LAGENNNU = "http://lagen.nu/terms#"
DCT = "http://purl.org/dc/terms/"
XSD = "http://www.w3.org/2001/XMLSchema#"

GRUNDLAG_NUMBERS = [
    '1974:152', // regeringsform
    '1810:0926', // successionsordningen
    '1949:105', // tryckfrihetsförordningen
    '1991:1469', // yttrandefrihetsgrundlagen
]


def ignoreSourceLine(l) {
    return l =~ /#.*S\d+>/ ||
        l.contains("<${LAGENNNU}senastHamtad>") ||
        l.contains("<${LAGENNNU}patchdescription>") ||
        l.contains('> ""') ||
        l.contains("<${DCT}references>")
        // TODO: ok att bara skippa f.n.?
        //- ta bort eller fixa references-"framåtpekningar", t.ex.::
        //<http://rinfo.lagrummet.se/publ/sfs/1962:381#L1988:881N11> .\+references> <http://rinfo.lagrummet.se/publ/sfs/1998:674> .
}



def convertLagenNuTripleGroups(String key, List<String> lines) {
    uri = fixUri(key)
    def results = [:]

    def newLines = rewriteLagenNuNTLines(lines)
    newLines = flipPartOfTriples(newLines)


    def typeTriplePosition = newLines.findIndexOf { it.startsWith("<${uri}> <${RDF.TYPE}> ") }

    if (typeTriplePosition == -1) {
        newLines += [
            "<${uri}> <${RDF.TYPE}> <${RPUBL}Forordning> .",
            "<${uri}> <${RPUBL}forfattningssamling> <${RINFO}serie/fs/sfs> .",
            "<${uri}> <http://purl.org/dc/terms/publisher> <${RINFO}org/regeringskansliet> .",
        ]
    }

    if (newLines[typeTriplePosition].endsWith("<${RPUBL}KonsolideradGrundforfattning> .")) {
        def konsLines = null
        def konsTypeTriple = newLines[typeTriplePosition]

        // retype: // TODO: only works if given "lag" is given as KonsolideradGrundforfattning
        if (GRUNDLAG_NUMBERS.find(uri.&endsWith)) {
            newLines[typeTriplePosition] = "<${uri}> <${RDF.TYPE}> <${RPUBL}Grundlag> ."
        } else if (isLaw(newLines)) {
            newLines[typeTriplePosition] = "<${uri}> <${RDF.TYPE}> <${RPUBL}Lag> ."
        } else {
            newLines[typeTriplePosition] = "<${uri}> <${RDF.TYPE}> <${RPUBL}Forordning> ."
        }

        (konsLines, newLines) = newLines.split {
            it.find("<${RPUBL}konsoliderar>") ||
                    it.find("<${RPUBL}konsolideringsunderlag>")
        }
        def konsUnderLag = konsLines.collect {
                it =~ "<${RPUBL}konsolideringsunderlag> <(.+)>" }.collect {
                        if (it) it[0][1] }.findAll { it }.sort()
        def lastKons = konsUnderLag[-1]
        konsLines << konsTypeTriple
        // TODO: find some real value for issued of the "konsolidering"..
        def issued = "${(lastKons =~ /(\d+):[^>]+/)[0][1]}-12-31"
        def konsUri = "${uri}/konsolidering/${issued}"
        konsLines += newLines.findAll {
            it.contains("<${DCT}title>") ||
                    it.contains("<${DCT}publisher>")
        }
        konsLines = konsLines.collect {
            replaceOnce(it, uri, konsUri)
        }
        def identifier = "SFS ${(uri =~ /(\d+:[^:]+)$/)[0][1]} i lydelse enligt SFS ${(lastKons =~ /(\d+:[^>]+)/)[0][1]}"
        konsLines << "<${konsUri}> <${DCT}identifier> \"${identifier}\"."
        konsLines << "<${konsUri}> <${DCT}issued> \"${issued}\"^^<${XSD}date> ."

        results[konsUri] = konsLines.join("\n")

    } else if (newLines[typeTriplePosition].endsWith("<${RPUBL}Rattsfallsreferat> .")) {
        newLines = newLines.collect {
            def s = replaceOnce(it, "<${DCT}description>", "<${RPUBL}referatrubrik>")
            // TODO: is "issued" good enough?
            s = replaceOnce(s, "<${RPUBL}avgorandedatum>", "<${DCT}issued>")
            s = s.replaceAll(/(.+) <${DCT}identifier> "\w+ ([^"]+)"@sv \./,
                            '$1 <'+RPUBL+'publikationsplatsangivelse> "$2" .')
            return s
        }
    }


    // TODO: document-level change ref:
    //- finns:
    //    dokument (ersatter|upphaver) dokument|kapitel|paragraf
    //    dokument inforsI dokument|kapitel|paragraf # <- obs; gäller då *del* som inforsI
    extra = []
    newLines.each {
        //(it =~ /> <.+?(?:ersatter|upphaver)> <([^#]+)#[^>]+> \./)?.each { m, o ->
        (it =~ "<${uri}> <${RPUBL}(?:ersatter|upphaver|inforsI)> <([^#]+)#[^>]+>")?.each { m, o ->
            extra << "<${uri}> <${RPUBL}andrar> <${o}> ."
        }
    }
    newLines += extra


    results[uri] = newLines.join("\n")

    return results
}


def rewriteLagenNuNTLines(lines) {
    return lines.collect {
        // TODO:? are org refs always correct?
        def line = replaceOnce(it, "<http://lagen.nu/org/2008/", "<${RINFO}org/")

        line = replaceOnce(line, "<${RINFO}ref/sfs>", "<${RINFO}serie/fs/sfs>")
        line = replaceOnce(line, "<${RINFO}ref/fs", "<${RINFO}serie/fs")
        line = replaceOnce(line, "<${RINFO}ref/rff", "<${RINFO}serie/rff")

        line = replaceOnce(line, "<${LAGENNNU}paragrafnummer>", "<${RPUBL}paragrafnummer>")
        line = replaceOnce(line, "<${LEGACY_PUBL}forfattningsamling>", "<${RPUBL}forfattningssamling>")
        line = replaceOnce(line, "<${LEGACY_PUBL}fsNummer> \"", "<${DCT}identifier> \"SFS ")


        line = replaceOnce(line, "<${LEGACY_PUBL}", "<${RPUBL}")
        line = line.replaceAll(/("\d{4}-\d{2}-\d{2}")(@\w+)?/, '$1'+"^^<${XMLSchema.NAMESPACE}date>")

        line = line.replaceAll(/#K(\d+)P/, '#k_$1-p_')
        line = line.replaceAll(/#K(\d+)/, '#k_$1')
        line = line.replaceAll(/#P(\d+)/, '#p_$1')

        line = fixUri(line)
        return line
    }
}


def flipPartOfTriples(lines) {
    return lines.collect {
        def partOfTriple = /(<[^>]+>) <${DCT}isPartOf> (<[^>]+>) \./
        if (!(it =~ partOfTriple))
            return it
        else if (it =~ /[#-]p_[^-]+>/)
            return it.replaceAll(partOfTriple, '$2 '+"<${RPUBL}paragraf>"+' $1 .')
        else if (it =~ /#k_[^-]+>/)
            return it.replaceAll(partOfTriple, '$2 '+"<${RPUBL}kapitel>"+' $1 .')
        else
            //println "DEBUG: couldn't flip: ${it}"
            null
    }.findAll { it }
}


def fixUri(line) {
    line = replaceOnce(line, "${RINFO}publ/rattsfall/", "${RINFO}publ/rf/")
    line = line.replaceAll(/${RINFO}publ\/rf\/(\w+)\/(\d+?)s(\d+)\b/, "${RINFO}publ/rf/"+'$1/$2/s_$3')
    // TODO: change e.g. </publ/sfs/1893:37_s.1> to </publ/sfs/1893:37_s_1>
    return line
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


/*=============================== main ===============================*/

try {
    createDepotFromTriples(new URI(RINFO), new File(args[0]), new File(args[1]),
            "-debug" in args)
} catch (IndexOutOfBoundsException e) {
    println "Usage: <rdf-source> <depot-dir>"
    System.exit 0
}

