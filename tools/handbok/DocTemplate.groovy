import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory

import com.uwyn.jhighlight.renderer.XhtmlRendererFactory


@Grab(group='com.uwyn', module='jhighlight', version='1.0')
class DocTemplate {

    static docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    def docUrl
    def tpltDocFile
    def variables

    DocTemplate(URL docUrl, tpltDocFile, variables) {
        this.docUrl = docUrl
        this.tpltDocFile = tpltDocFile
        this.variables = variables
    }

    Document render() {
        def doc = getDocument(docUrl)
        if (tpltDocFile) {
            doc = fillTemplate(getDocument(tpltDocFile), doc)
        }
        processIncludeDirectives(doc)
        setValues(doc, variables)
        highlightSourceBlocks(doc)
        return doc
    }


    static Document getDocument(InputStream inputStream) {
        def doc = docBuilder.parse(inputStream)
        inputStream.close()
        return doc
    }

    static Document getDocument(URL url) {
        return getDocument(url.openStream())
    }

    static Document getDocument(File file) {
        return getDocument(new FileInputStream(file))
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
            if (elem) {
                elem.setTextContent(value.toString())
            }
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
        // TODO: opt. ref to url or "named ref" to document in variables
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

}
