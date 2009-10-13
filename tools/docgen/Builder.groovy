package docgen

import java.text.SimpleDateFormat
import groovy.xml.dom.DOMUtil


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

    Builder(resourceDir, sourceDir, buildDir) {
        this.resourceDir = resourceDir
        this.sourceDir = sourceDir
        this.buildDir = buildDir
    }

    void build(boolean clean=false,
            renderPatterns=DEFAULT_RENDER_PATTERNS,
            copyPatterns=DEFAULT_COPY_PATTERNS)
    {
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

        def systemIdMap = [
            "http://www.w3.org/TR/xhtml1/DTD/": "${getClass().getResource("dtd").path}/"
        ]
        def customProtocols = [
            "build": buildDir
        ]
        def svnVersionNumber = getSvnVersionNumber()

        def dataView = new DataViewer(
                ["${resourceDir}/base/model",
                "${resourceDir}/base/extended/rdf",
                "${resourceDir}/external/rdf"] as String[]
        )
        def modelPath = buildDir+"/model"
        def modelHtmlPath = modelPath+'.xhtml'
        dataView.renderModel(modelHtmlPath, "sv")
        new PdfMaker().renderAsPdf(
                new DocTemplate(systemIdMap).render(
                        new File(modelHtmlPath).toURL()),
                new File(modelPath+'.pdf'))

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
            def outUrl = new URL(inUrl.toString().replace(srcPath, buildPath))
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
            outFile.withOutputStream {
                DOMUtil.serialize(doc.documentElement, it)
            }
            new PdfMaker().renderAsPdf(doc,
                    new File(outFile.canonicalPath.replace('.xhtml', '.pdf')))
        }

    }

    static String relRoot(String base, File file) {
        def path = file.parentFile.toURL() as String
        if (path.startsWith(base)) {
            return '../' * path.replace(base, "").count('/')
        }
    }

    static String normUrlPath(filePath) {
        return new File(filePath).canonicalFile.toURL().toString()
    }

    static String getSvnVersionNumber() {
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
