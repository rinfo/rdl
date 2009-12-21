package docgen

import java.text.SimpleDateFormat
// TODO: deprecated; use: <http://xerces.apache.org/xerces2-j/faq-general.html#faq-6>
import org.apache.xml.serialize.OutputFormat
import org.apache.xml.serialize.XHTMLSerializer
import org.apache.xml.serialize.XMLSerializer

import se.lagrummet.rinfo.base.rdf.RDFUtil


class Builder {

    static void main(String[] args) {
        def (flags, paths) = args.split { it =~ /^--/ }
        if (paths.size() < 3) {
            println "Usage: %prog <resources> <src> <dest>"
            System.exit 1
        }
        def resourceDir = paths[0]
        def sourceDir = paths[1]
        def buildDir = paths[2]
        def clean = "--clean" in flags
        new Builder(resourceDir, sourceDir, buildDir).
                build(clean, DEFAULT_RENDER_PATTERNS, DEFAULT_COPY_PATTERNS)
    }

    static DEFAULT_COPY_PATTERNS = [
            "css/*.*", "img/*.*"
        ]
    static DEFAULT_RENDER_PATTERNS = [
            "index.xhtml",
            "handbok/**.xhtml",
            "system/**.xhtml"
        ]

    String resourceDir
    String sourceDir
    String buildDir
    Map systemIdMap
    Map customProtocols

    Builder(resourceDir, sourceDir, buildDir) {
        this.resourceDir = resourceDir
        this.sourceDir = sourceDir
        this.buildDir = buildDir
        systemIdMap = [
            "http://www.w3.org/TR/xhtml1/DTD/": "${getClass().getResource("dtd").path}/"
        ]
        customProtocols = [
            "build": buildDir
        ]
    }

    void build(renderPatterns=DEFAULT_RENDER_PATTERNS,
            copyPatterns=DEFAULT_COPY_PATTERNS,
            clean=false, generate=true) {
        def ant = new AntBuilder()

        if (clean) {
            ant.delete(dir:buildDir)
        }
        ant.mkdir(dir:buildDir)

        ant.copy(todir:buildDir) {
            fileset(dir:sourceDir) {
                copyPatterns.each {
                    include(name: it)
                }
            }
        }

        def svnVersionNumber = getSvnVersionNumber()

        if (generate) {
            generateBaseViews()
        }

        def srcPath = normUrlPath(sourceDir)
        def buildPath = normUrlPath(buildDir)

        ant.fileScanner {
            fileset(dir:sourceDir) {
                renderPatterns.each {
                    include(name: it)
                }
            }
        }.each {
            def inUrl = it.toURL()
            def outUrl = new URL(inUrl.toString()
                    .replace(srcPath, buildPath)
                    .replace('.xhtml', '.html'))
            ant.echo "<${inUrl}> => <${outUrl}>"

            def params = [
                docdate: new SimpleDateFormat("yyyy-MM-dd").format(
                        new Date(it.lastModified() )),
                svnversion: svnVersionNumber,
                root: relRoot(srcPath, it)
            ]
            def tplt = new DocTemplate(systemIdMap, customProtocols, params)
            def doc = tplt.renderAndHighlightCode(inUrl)

            def outFile = new File(outUrl.toURI())
            ant.mkdir(dir:outFile.parentFile)
            writeXhtml(doc, outFile)
            writePdf(doc, new File(outFile.canonicalPath.replace('.html', '.pdf')))
        }

    }

    void writeXhtml(doc, outFile) {
        outFile.withOutputStream {
            new XHTMLSerializer(it, new OutputFormat(doc)).serialize(doc)
        }
    }

    void writePdf(doc, outFile) {
        new PdfMaker().renderAsPdf(doc, outFile)
    }

    static relRoot(String base, File file) {
        def path = file.parentFile.toURL() as String
        if (path.startsWith(base)) {
            return '../' * path.replace(base, "").count('/')
        }
    }

    static normUrlPath(filePath) {
        return new File(filePath).canonicalFile.toURL().toString()
    }

    static getSvnVersionNumber() {
        def proc = "svn info --xml".execute()
        proc.waitFor()
        if (proc.exitValue() == 0) {
            def svnInfoDoc = new XmlParser().parseText(proc.in.text)
            return svnInfoDoc.entry.'@revision'.text()
        } else {
            return "?"
        }
    }

    void generateBaseViews() {
            generateModelXhtml()
            generateUriSchemeXhtml()
    }

    void generateModelXhtml() {
        def dataView = new DataViewer(
                ["${resourceDir}/base/model",
                "${resourceDir}/base/extended/rdf",
                "${resourceDir}/external/rdf"] as String[]
        )
        def modelPath = buildDir+"/model"
        def modelHtmlPath = modelPath+'.xhtml'
        dataView.renderModel(modelHtmlPath, "sv")
        def doc = new DocTemplate(systemIdMap).render(
                new File(modelHtmlPath).toURL())
        new PdfMaker().renderAsPdf(doc, new File(modelPath+'.pdf'))
    }

    void generateUriSchemeXhtml() {
        def repo = RDFUtil.slurpRdf("${resourceDir}/base/sys/uri/",
                "${resourceDir}/base/model/rinfo_publ.n3",
                "${resourceDir}/base/extended/rdf/"
            )
        //new File("/tmp/coin-sesame_ser.rdf").withOutputStream {
        //    RDFUtil.serialize(repo, "application/rdf+xml", it)
        //}
        //return
        def rdfXmlInput = RDFUtil.toInputStream(repo, "application/rdf+xml")

        def doc = new DocTemplate(systemIdMap).transform(rdfXmlInput,
                "${resourceDir}/external/xslt/rdfxml-grit.xslt",
                "${sourceDir}/templates/coinscheme.xslt")
        //new File("/tmp/urischeme.grit").withOutputStream {
        //    new XMLSerializer(it, new OutputFormat(doc)).serialize(doc)
        //}
        writeXhtml(doc, new File(buildDir+"/urischeme.xhtml"))
    }

}
