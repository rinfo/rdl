import org.apache.commons.io.FileUtils
import org.apache.abdera.Abdera
import org.springframework.context.support.ClassPathXmlApplicationContext as Ctxt
import se.lagrummet.rinfo.store.depot.*


def sourceRdfDir
try {
    sourceRdfDir = new File(args[0])
} catch (IndexOutOfBoundsException e) {
    println "Usage: <rdf-dir-to-import>"
    System.exit 0
}

final MTYPE_RDF = "application/rdf+xml"

context = new Ctxt("applicationContext.xml")
depot = context.getBean("fileDepot")

FileUtils.iterateFiles(sourceRdfDir, ["rdf"] as String[], true).each {
    def entryFile = new File(it.parentFile, "entry.atom")
    def entry = Abdera.instance.parser.parse(new FileInputStream(entryFile)).root
    def id = entry.id.toURI()
    def date = entry.updated
    println "Importing rdf file <${it}> as <${id}> [${date}]"
    try {
        depot.createEntry(id, date, [new SourceContent(it, MTYPE_RDF)])
    } catch (DuplicateDepotEntryException e) {
        println "Couldn't add duplicate: ${e.message}"
    }
}
