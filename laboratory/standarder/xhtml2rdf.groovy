// require(url='http://xml.apache.org/xalan-j/', jar='serializer.jar')
// require(url='http://xml.apache.org/xalan-j/', jar='xalan_270.jar')
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

if (args.length < 2) {
    println "Usage: %prog <xhtml-file> <xslt-file>"
    System.exit 1
}

def xhtmlfile = new FileReader(args[0])
def xsltfile = new FileInputStream(args[1])

xhtml = ""
print groovy.xml.DOMBuilder.parse(xhtmlfile, false, false)

xslt = xsltfile.text

def factory = TransformerFactory.newInstance()
def transformer = factory.newTransformer(new StreamSource(new StringReader(xslt)))
transformer.transform(new StreamSource(new StringReader(xhtml)), new StreamResult(System.out))
