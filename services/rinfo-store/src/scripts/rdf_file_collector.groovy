import org.apache.commons.io.FileUtils as FU
import org.apache.abdera.Abdera
import se.lagrummet.rinfo.store.depot.*


if (args.size() != 3) {
    println "Usage: <script> <base-uri> <depot-dir> <rdf-dir-to-import>"
    System.exit 0
}

final MTYPE_RDF = "application/rdf+xml"


def baseUri = new URI(args[0])
def depotDir = new File(args[1])
def sourceRdfDir = new File(args[2])


def fileDepot = new FileDepot(baseUri, depotDir)


FU.iterateFiles(sourceRdfDir, ["rdf"] as String[], true).each {
    def entryFile = new File(it.parentFile, "entry.atom")
    def entry = Abdera.instance.parser.parse(new FileInputStream(entryFile)).root
    def id = entry.id.toURI()
    def date = entry.updated
    println "Importing rdf file <${it}> as <${id}> [${date}]"
    try {
        fileDepot.createEntry(id, date, [new DepotContent(it, null, MTYPE_RDF)])
    } catch (DuplicateDepotEntryException e) {
        println "Couldn't add duplicate: ${e.message}"
    }
}


