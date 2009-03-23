import org.xhtmlrenderer.pdf.ITextRenderer
import com.lowagie.text.DocumentException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import com.uwyn.jhighlight.renderer.XhtmlRendererFactory
import com.uwyn.jhighlight.tools.FileUtils


@Grab(group='com.lowagie', module='itext', version='2.0.8')
@Grab(group='org.xhtmlrenderer', module='core-renderer', version='R8pre2')
@Grab(group='com.uwyn', module='jhighlight', version='1.0')
class HandbookMaker {

    static void main(String[] args) {
        if (args.length != 2) {
            println "Usage: %prog <input-file> <output-file>"
            System.exit 1
        }
        def inputFilename = args[0]
        def pdfFilename = args[1]
        def doc = getInputDocument(inputFilename)
        def renderer = createRenderer()
        renderer.setDocument(doc, null)
        renderer.layout()
        def os = new FileOutputStream(pdfFilename)
        renderer.createPDF(os)
        os.close()
    }

    static Document getInputDocument(inputFilename) {
        def infile = new File(inputFilename)
        def docXml = processIncludeDirectives(infile.getText("UTF-8"))
        def inputStream = new ByteArrayInputStream(docXml.getBytes("UTF-8"))
        def doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        inputStream.close()
        return doc
    }

    /**
    * Finds "div.sourcecode" sections loads, referenced source files, highlights
    * them and then puts them inline.
    */
    static String processIncludeDirectives(docText) {
        (docText =~ /<div class="sourcecode">(.*)<\/div>/).each {match ->
            def source = new File(match[1]).getText("UTF-8")
            // Pick renderer based on file extension
            def hlRenderer = XhtmlRendererFactory.getRenderer(
                    FileUtils.getExtension("test.xml"))
            def hlSourceFragment = hlRenderer.highlight("", source, "iso-8859-1", true)
            //Replace sourcecode section with highlighted source
            docText = docText.replace("<div class=\"sourcecode\">" + match[1] + "</div>",
                    "<div class=\"sourcecode\">\n\n" + hlSourceFragment + "\n\n</div>")
        }
        return docText
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
