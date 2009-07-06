import java.text.SimpleDateFormat

import groovy.xml.dom.DOMUtil
import groovy.xml.dom.DOMCategory

import com.uwyn.jhighlight.renderer.XhtmlRendererFactory


@Grab(group='com.uwyn', module='jhighlight', version='1.0')
class HandbookMaker {

    static void main(String[] args) {
        // TODO: fill data dir with generated includable xhtml here?
        // TODO: use given file *or* walk dir
        if (args.length < 2) {
            println "Usage: %prog <input-url> <output-file>"
            System.exit 1
        }
        def inUrl = args[0].with {new URL(it =~ /^https?:/ ? it : "file:"+it)}
        def outFile = new File(args[1])
        renderDocument(inUrl, outFile)
    }

    static renderDocument(URL inUrl, File outFile) {
        def systemIdMap = [
            "http://www.w3.org/TR/xhtml1/DTD/": "dtd"
        ]
        def params = [
            docdate: new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
            svnversion: getSvnVersionNumber()
        ]

        def docTemplate = new DocTemplate(systemIdMap, params)

        def doc = docTemplate.parse(inUrl)
        highlightSourceBlocks(docTemplate.docBuilder, doc)

        if (outFile.name.endsWith("pdf")) {
            def out = new FileOutputStream(outFile)
            PdfMaker.renderAsPdf(doc, outFile.toURL().toString(), out)
            out.close()
        } else {
            def out = outFile.name == "-" ? System.out : new FileOutputStream(outFile)
            DOMUtil.serialize(doc.documentElement, out)
            out.close()
        }
    }

    static String getSvnVersionNumber() {
        def proc = "svn info --xml".execute()
        proc.waitFor()
        if (proc.exitValue() == 0) {
            def svnInfoDoc = new XmlParser().parseText(proc.in.text)
            return svnInfoDoc.entry.'@revision'.text()
        } else {
            return "0"
        }
    }

    /**
    * Replaces plain source code text with highlighted markup.
    */
    static void highlightSourceBlocks(docBuilder, doc) {
        use (DOMCategory) {
            doc.documentElement.'**'.'code'.each { code ->
                def hlRenderer = XhtmlRendererFactory.getRenderer(code.'@class')
                if (hlRenderer) {
                    def hlDoc = docBuilder.parse(new ByteArrayInputStream(
                            ('<!DOCTYPE html SYSTEM "dtd/xhtml1-strict.dtd"><code>' +
                            hlRenderer.highlight("", code.text(), "iso-8859-1", true) +
                            '</code>').bytes)
                        )
                    code.setTextContent("")
                    hlDoc.documentElement.'*'.each {
                        code.appendChild(doc.importNode(it, true))
                    }
                }
            }
        }
    }

}
