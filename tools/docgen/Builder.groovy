package docgen

import java.text.SimpleDateFormat
import groovy.xml.dom.DOMUtil


@Grab(group='com.uwyn', module='jhighlight', version='1.0')
@Grab(group='com.lowagie', module='itext', version='2.0.8')
@Grab(group='org.xhtmlrenderer', module='core-renderer', version='R8pre2')
class Builder {

    static void main(String[] args) {
        def (flags, paths) = args.split { it =~ /^--/ }
        if (paths.size() < 3) {
            println "Usage: %prog <resources> <src> <dest>"
            System.exit 1
        }
        def resourceDir = paths[0]
        def srcDir = paths[1]
        def buildDir = paths[2]
        def clean = "--clean" in flags
        build(srcDir, buildDir, resourceDir, clean)
    }

    static build(srcDir, buildDir, resourceDir, clean=false) {
        def ant = new AntBuilder()

        if (clean) {
            ant.delete(dir:buildDir)
        }
        ant.mkdir(dir:buildDir)

        ant.copy(todir:buildDir) {
            fileset(dir:srcDir) {
                include(name:"css/*.*")
                include(name:"img/*.*")
            }
        }

        def srcPath = normUrlPath(srcDir)
        def buildPath = normUrlPath(buildDir)

        def systemIdMap = [
            "http://www.w3.org/TR/xhtml1/DTD/": "${getResource("dtd").path}/"
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
        dataView.renderModel(buildDir+"/model.xhtml")

        ant.fileScanner {
            fileset(dir:srcDir) {
                include(name: "handbok/**.xhtml")
                include(name: "system/**.xhtml")
            }
        }.each {

            def inUrl = it.toURL()
            def outUrl = new URL(inUrl.toString().replace(srcPath, buildPath))
            ant.echo "<${inUrl}> => <${outUrl}>"

            def params = [
                docdate: new SimpleDateFormat("yyyy-MM-dd").format(
                        new Date(it.lastModified() )),
                svnversion: svnVersionNumber
            ]
            def tplt = new DocTemplate(params, systemIdMap, customProtocols)
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

    static normUrlPath(filePath) {
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
