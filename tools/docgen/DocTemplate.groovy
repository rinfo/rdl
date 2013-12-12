package docgen

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamSource

import org.w3c.dom.Document
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

import groovy.xml.dom.DOMCategory

import com.uwyn.jhighlight.renderer.XhtmlRendererFactory


class DocTemplate {

    DocumentBuilder docBuilder
    TransformerFactory tFactory

    Map systemIdMap
    Map customProtocols
    Map params

    DocTemplate(systemIdMap=null, customProtocols=null, params=null) {
        this.systemIdMap = systemIdMap
        this.customProtocols = customProtocols
        this.params = params
        def dbf = DocumentBuilderFactory.newInstance()
        dbf.setNamespaceAware(true)
        dbf.setXIncludeAware(true)
        docBuilder = dbf.newDocumentBuilder()
        docBuilder.entityResolver = entityResolver
        tFactory = TransformerFactory.newInstance()
    }

    /**
     * Parses the document at the given url, expanding any xinclude directives
     * and transforming it if an xslt stylsheet instruction is present.
     */
    Document render(URL docUrl) {
        def doc = getDocument(docUrl)
        def domSource = new DOMSource(doc, docUrl.toString())
        def stylesheet = tFactory.getAssociatedStylesheet(domSource, null, null, null)
        if (stylesheet) {
            def transformer = tFactory.newTransformer(stylesheet)
            def domResult = new DOMResult()
            params?.each { key, value ->
                transformer.setParameter(key, value)
            }
            transformer.transform(domSource, domResult)
            return (Document) domResult.getNode()
        } else {
            return doc
        }
    }

    Document transform(InputStream inputStream, String... xslts) {
        def saxTransFctry = (SAXTransformerFactory) tFactory
        def domResult = new DOMResult()
        def filter = null
        for (String xslt : xslts) {
            def tplt = tFactory.newTemplates(new StreamSource(xslt))
            def nextFilter = saxTransFctry.newXMLFilter(tplt)
            if (filter) nextFilter.setParent(filter)
            filter = nextFilter
        }
        def transformSource = new SAXSource(filter, new InputSource(inputStream))
        def chainedTransformer = saxTransFctry.newTransformer()
        chainedTransformer.transform(transformSource, domResult)
        return (Document) domResult.getNode()
    }

    /**
     * Uses {@link render} and invokes {@highlightSourceBlocks} on the results.
     */
    Document renderAndHighlightCode(URL docUrl) {
        def doc = render(docUrl)
        highlightSourceBlocks(doc)
        return doc
    }

    /**
    * Replaces plain source code text with highlighted markup.
    */
    void highlightSourceBlocks(doc) {
        use (DOMCategory) {
            doc.documentElement.'**'.'code'.each { code ->
                def hlRenderer = XhtmlRendererFactory.getRenderer(code.'@class')
                if (hlRenderer) {
                    def hlDoc = docBuilder.parse(new ByteArrayInputStream(
                            (
                            '''<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">''' +
                            '<code>' +
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

    protected Document getDocument(URL url) {
        return getDocument(url.openStream(), url.toString())
    }

    protected Document getDocument(File file) {
        return getDocument(new FileInputStream(file), file.toURL().toString())
    }

    protected Document getDocument(InputStream inputStream, String systemId) {
        def doc = docBuilder.parse(inputStream, systemId)
        inputStream.close()
        return doc
    }

    protected def entityResolver = { publicId, systemId ->
        def resolved = null
        systemIdMap?.each { base, dir ->
            if (systemId.startsWith(base)) {
                resolved = systemId.replace(base,
                        new File(dir).toURL().toString())
            }
        }
        customProtocols?.each { base, dir ->
            def proto = base+':'
            if (systemId.startsWith(proto)) {
                def path = systemId.substring(proto.size())
                resolved = new File(dir, path).canonicalPath
            }
        }
        return resolved? new InputSource(resolved) : null
    } as EntityResolver

}
