import static org.apache.commons.io.FileUtils.iterateFiles
import org.apache.commons.io.filefilter.HiddenFileFilter
import org.apache.commons.io.filefilter.IOFileFilter
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.abdera.Abdera
import org.springframework.context.support.ClassPathXmlApplicationContext as Ctxt
import se.lagrummet.rinfo.store.depot.*


def sourceDir
def entryInfoFileName
String[] suffixes
try {
    sourceDir = new File(args[0])
    entryInfoFileName = args[1]
    if (args.length > 2) {
        suffixes = args[2].split(",")
    }
} catch (IndexOutOfBoundsException e) {
    println "Usage: <source-dir> <entry-info-file> [suffix,...]"
    System.exit 0
}

context = new Ctxt("applicationContext.xml")
depot = context.getBean("fileDepot")

iterateFiles(sourceDir,
        new NameFileFilter(entryInfoFileName),
        HiddenFileFilter.VISIBLE).each {

    def entry = Abdera.instance.parser.parse(new FileInputStream(it)).root
    def contentDir = it.parentFile
    def id = entry.id.toURI()
    def date = entry.published ?: entry.updated
    def contents = iterateFiles(contentDir, suffixes, false).collect {
        new SourceContent(it, depot.computeMediaType(it))
    }
    println "Importing entry <${id}> [${date}] from <${contentDir}>"
    try {
        depot.createEntry(id, date, contents)
    } catch (DuplicateDepotEntryException e) {
        println "Error: couldn't add duplicate: ${e.message}"
    }

}
