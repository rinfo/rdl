package docgen

import java.text.SimpleDateFormat
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

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
        def patterns = paths[3]? paths[3..-1] : DEFAULT_RENDER_PATTERNS
        def copies = DEFAULT_COPY_PATTERNS
        def clean = "--clean" in flags
        def nogen = "--nogen" in flags
        new Builder(resourceDir, sourceDir, buildDir).
                build(patterns, copies, clean, !nogen)
    }

    static DEFAULT_COPY_PATTERNS = [
            "css/*.*", "img/*.*"
        ]
    static DEFAULT_RENDER_PATTERNS = [
            "index.xhtml",
            "handbok/**.xhtml",
            "introduktion/**.xhtml",
            "system/**.xhtml"
        ]

    def fontDir = "fonts"
    def fontFiles = [
            "gara.ttf", "garait.ttf", "garabd.ttf",
            "trebuc.ttf", "trebucit.ttf", "trebucbi.ttf", "trebucbd.ttf"
    ]

    String resourceDir
    String sourceDir
    String buildDir
    Map systemIdMap
    Map customProtocols
    PdfMaker pdfMaker

    Builder(resourceDir, sourceDir, buildDir) {
        this.resourceDir = resourceDir
        this.sourceDir = sourceDir
        this.buildDir = buildDir
        def dtdDir = "${getClass().getResource("dtd")?.path ?: "docgen/dtd"}/"
        systemIdMap = [
            "http://www.w3.org/TR/xhtml1/DTD/": dtdDir
        ]
        customProtocols = [
            "build": buildDir
        ]

        def fonts = fontFiles.collect { new File(fontDir, it).path }
        pdfMaker = new PdfMaker(fonts)
    }

    void build(renderPatterns=DEFAULT_RENDER_PATTERNS,
            copyPatterns=DEFAULT_COPY_PATTERNS,
            clean=false, generate=true, examplesZip=true) {
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

        if (examplesZip) {
            ant.mkdir(dir:"${buildDir}/handbok")
            ant.zip(basedir:"${sourceDir}/exempel",
                    includes:"**/*.atom **/*.rdf **/*.xhtml **/*.pdf",
                    destfile:"${buildDir}/handbok/exempel.zip")
        }

        def svnVersionNumber = getSvnVersionNumber()

        if (generate) {
            generateBaseDocs()
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

    void generateBaseDocs() {
        // TODO: use GritTransformer from base
        transformRdfToXhtml(
                ["${resourceDir}/base/model",
                    "${resourceDir}/base/extended/rdf",
                    "${resourceDir}/external/rdf"],
                ["${resourceDir}/external/xslt/grit/rdfxml-grit.xslt",
                    "${sourceDir}/templates/model-grit_to_xhtml.xslt"],
                buildDir+"/model.xhtml")

        transformRdfToXhtml(
                ["${resourceDir}/base/sys/uri/",
                    "${resourceDir}/base/datasets/",
                    "${resourceDir}/base/model/rinfo_publ.n3",
                    "${resourceDir}/base/extended/rdf/"],
                ["${resourceDir}/external/xslt/grit/rdfxml-grit.xslt",
                    "${sourceDir}/templates/coinscheme.xslt"],
                buildDir+"/urischeme.xhtml", false)
    }

    void transformRdfToXhtml(rdfPaths, xslts, outPath, makePdf=true) {
        def repo = RDFUtil.slurpRdf(rdfPaths as String[])
        def rdfXmlInput = RDFUtil.toInputStream(repo, "application/rdf+xml")
        def doc = new DocTemplate(systemIdMap).transform(rdfXmlInput,
                xslts as String[])
        def outFile = new File(outPath)
        writeXhtml(doc, outFile)
        if (makePdf) {
            writePdf(doc, new File(outFile.canonicalPath.replace('.xhtml', '.pdf')))
        }
    }

    void writeXhtml(doc, outFile) {
        def t = TransformerFactory.newInstance().newTransformer()
        [   (OutputKeys.METHOD): "xml",
            (OutputKeys.OMIT_XML_DECLARATION): "yes",
            (OutputKeys.DOCTYPE_PUBLIC): "-//W3C//DTD XHTML 1.0 Strict//EN",
            (OutputKeys.DOCTYPE_SYSTEM): "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
        ].each t.&setOutputProperty
        outFile.withOutputStream {
            t.transform(new DOMSource(doc), new StreamResult(it))
        }
    }

    void writePdf(doc, outFile) {
        pdfMaker.renderAsPdf(doc, outFile)
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

}
