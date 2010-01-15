package docgen

import java.text.SimpleDateFormat
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.apache.commons.configuration.ConfigurationUtils
import net.sf.json.groovy.JsonSlurper
import org.antlr.stringtemplate.StringTemplateGroup

import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.ModelViewHandler


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
        generateModelDocs(
                ["${resourceDir}/base/model",
                    "${resourceDir}/base/extended/rdf",
                    "${resourceDir}/external/rdf"],
                buildDir+"/model")
        transformRdfToXhtml(
                ["${resourceDir}/base/sys/uri/",
                    "${resourceDir}/base/model/rinfo_publ.n3",
                    "${resourceDir}/base/extended/rdf/"],
                ["${resourceDir}/external/xslt/rdfxml-grit.xslt",
                    "${sourceDir}/templates/coinscheme.xslt"],
                buildDir+"/urischeme.xhtml")
    }

    void transformRdfToXhtml(rdfPaths, xslts, outPath) {
        def repo = RDFUtil.slurpRdf(rdfPaths as String[])
        def rdfXmlInput = RDFUtil.toInputStream(repo, "application/rdf+xml")
        def doc = new DocTemplate(systemIdMap).transform(rdfXmlInput,
                xslts as String[])
        writeXhtml(doc, new File(outPath))
    }

    void generateModelDocs(rdfPaths, outBasePath) {
        def repo = RDFUtil.slurpRdf(rdfPaths as String[])
        def modelHtmlPath = outBasePath+'.xhtml'
        renderModel(repo, modelHtmlPath, "sv")
        def doc = new DocTemplate(systemIdMap).render(
                new File(modelHtmlPath).toURL())
        new PdfMaker().renderAsPdf(doc, new File(outBasePath+'.pdf'))
    }

    static renderModel(repo, fname, locale="en", mediabase=".") {
        def templates = new StringTemplateGroup("sparqltrees")
        def rqViewer = new SparqlTreeViewer(repo, templates,
                "sparqltrees/model/model-tree-rq", "sparqltrees/model/model-html")
        def labelTree = new JsonSlurper().parse(ConfigurationUtils.locate(
                        "sparqltrees/model/model-settings.json"))
        def options = [ mediabase: mediabase ]
        def s = rqViewer.execute(new ModelViewHandler(locale, null, labelTree, options))
        println "Writing to: $fname"
        new File(fname).text = s
    }

    static writeXhtml(doc, outFile) {
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

    static writePdf(doc, outFile) {
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

}
