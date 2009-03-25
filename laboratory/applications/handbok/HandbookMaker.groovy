import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.xml.dom.DOMUtil

import java.text.SimpleDateFormat

import org.xhtmlrenderer.pdf.ITextRenderer
import com.uwyn.jhighlight.renderer.XhtmlRendererFactory
import com.uwyn.jhighlight.tools.FileUtils


@Grab(group='com.lowagie', module='itext', version='2.0.8')
@Grab(group='org.xhtmlrenderer', module='core-renderer', version='R8pre2')
@Grab(group='com.uwyn', module='jhighlight', version='1.0')
class HandbookMaker {

    static DocumentBuilder docBuilder = DocumentBuilderFactory.
            newInstance().newDocumentBuilder()

    static void main(String[] args) {
        if (args.length < 2) {
            println "Usage: %prog <input-file> <output-file>"
            System.exit 1
        }
        def inFile = new File(args[0])
        def outFile = new File(args[1])
        def doc = processDocument(
                getDocument(inFile),
                (args.length == 3)? getDocument(new File(args[2])) : null)

        if (outFile.name.endsWith("pdf")) {
            renderAsPdf(doc, outFile)
        } else {
            def out = outFile.name == "-" ? System.out : new FileOutputStream(outFile)
            DOMUtil.serialize(doc.documentElement, out)
        }
    }


    static String getSvnVersionNumber() {
        def command = """svn info --xml"""
        def proc = command.execute()
        proc.waitFor()  
        if (proc.exitValue() == 0) {
            def svninfo_doc = new XmlParser().parseText(proc.in.text)
            return svninfo_doc.entry.'@revision'.text()
        } else {
            return "0"
        }
    }


    static Document processDocument(doc, tpltDoc) {
        if (tpltDoc) {
            doc = fillTemplate(tpltDoc, doc)
        }
        processIncludeDirectives(doc)
        setValues(doc, [
            docdate: new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
            svnversion: getSvnVersionNumber()
        ])
        highlightSourceBlocks(doc)
        return doc
    }

    static Document getDocument(file) {
        def inputStream = new FileInputStream(file)
        def doc = docBuilder.parse(inputStream)
        inputStream.close()
        return doc
    }

    static Document fillTemplate(tpltDoc, doc) {
        tpltDoc = tpltDoc.cloneNode(true)
        use (DOMCategory) {
            def title = tpltDoc.documentElement.'**'.'title'[0]
            def titleExtra = doc.documentElement.'**'.'title'.text()
            if (title) {
                title.setTextContent(title.text() + titleExtra)
            }
            def body = tpltDoc.getElementById("body")
            if (body) {
                doc.getElementsByTagName("body").'*'.each {
                    body.appendChild(tpltDoc.importNode(it, true))
                }
            }
        }
        return tpltDoc
    }

    static void setValues(doc, map) {
        map.each { key, value ->
            def elem = doc.getElementById(key)
            elem.setTextContent(value.toString())
        }
    }


    /**
    * Finds "object.sourcecode" elements and includes referenced files inline.
    */
    static void processIncludeDirectives(doc) {
        use (DOMCategory) {
            doc.documentElement.'**'.'object'.each {
                if (it.'@class' =~ /\binclude\b/) {
                    replaceWithText(doc, it, it.'@src')
                } else if (it.'@class' =~ /\bxinclude\b/) {
                    replaceWithContent(doc, it, it.'@src')
                }
            }
        }
    }

    static void replaceWithText(doc, elem, srcRef) {
        def srcFile = new File(srcRef)
        if (!srcFile.isFile())
            return
        elem.parentNode.replaceChild(
                doc.createTextNode(srcFile.text), elem)
    }

    static void replaceWithContent(doc, elem, srcRef) {
        def (ref, fragment) = srcRef.split("#")
        def srcFile = new File(ref)
        if (!srcFile.isFile())
            return
        def inclDoc = getDocument(srcFile)
        def inclElem = fragment?
                inclDoc.getElementById(fragment) : inclDoc.documentElement
        elem.parentNode.replaceChild(
                doc.importNode(inclElem, true), elem)
    }

    /**
    * Replaces plain source code text with highlighted markup.
    */
    static void highlightSourceBlocks(doc) {
        use (DOMCategory) {
            doc.documentElement.'**'.'code'.each { code ->
                def hlRenderer = XhtmlRendererFactory.getRenderer(code.'@class')
                if (hlRenderer) {
                    def hlDoc = new DOMBuilder(doc).parseText(
                            hlRenderer.highlight("", code.text(), "iso-8859-1", false))
                    code.setTextContent("")
                    hlDoc.documentElement.'**'.'code'.'*'.each {
                        code.appendChild(doc.importNode(it, true))
                    }
                }
            }
        }
    }

    static void renderAsPdf(doc, outFile) {
        def renderer = createRenderer()
        renderer.setDocument(doc, null)
        renderer.layout()
        def os = new FileOutputStream(outFile)
        renderer.createPDF(os)
        os.close()
    }

    static ITextRenderer createRenderer() {
        def renderer = new ITextRenderer()
        //Add font variants for embedding
        [
            "gara.ttf", "garait.ttf", "garabd.ttf",
            "trebuc.ttf", "trebucit.ttf", "trebucbi.ttf", "trebucbd.ttf"
        ].each {
            if (new File(it).exists())
                renderer.fontResolver.addFont(it, true)
        }
        return renderer
    }

}
