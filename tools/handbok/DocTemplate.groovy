import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource

import org.w3c.dom.Document
import org.xml.sax.EntityResolver

import org.xml.sax.InputSource


class DocTemplate {

    DocumentBuilder docBuilder
    TransformerFactory tFactory

    Map systemIdMap
    Map params

    DocTemplate(systemIdMap, params) {
        this.systemIdMap = systemIdMap
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
     * and optionally transforming it if an xslt stylsheet instruction is
     * present.
     */
    Document parse(URL docUrl) {
        def doc = getDocument(docUrl)
        def domSource = new DOMSource(doc, docUrl.toString())
        def stylesheet = tFactory.getAssociatedStylesheet(domSource, null, null, null)
        if (stylesheet) {
            def transformer = tFactory.newTransformer(stylesheet)
            def domResult = new DOMResult()
            params.each { key, value ->
                transformer.setParameter(key, value)
            }
            transformer.transform(domSource, domResult)
            return (Document) domResult.getNode()
        } else {
            return doc
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
        systemIdMap.each { base, dir ->
            if (systemId.startsWith(base)) {
                resolved = systemId.replace(base,
                        new File(dir).toURL().toString())
            }
        }
        return resolved? new InputSource(resolved) : null
    } as EntityResolver

}
