import org.xhtmlrenderer.pdf.ITextRenderer
import com.lowagie.text.DocumentException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import com.uwyn.jhighlight.renderer.XhtmlRendererFactory
import com.uwyn.jhighlight.tools.FileUtils

input_file = "exempel.xhtml"
pdf_filename = "exempel.pdf"
infile = new File(input_file) 
doc_xml = infile.getText("UTF-8")

//Find div.sourcecode sections and load references highlighted source files
(doc_xml =~ /<div class=\"sourcecode\">(.*)<\\/div>/).each {match -> 

    source= new File(match[1]).getText("UTF-8")

    hl_renderer = XhtmlRendererFactory.getRenderer(FileUtils.getExtension("test.xml")) //Picks renderer based on file extension
    hl_source_fragment = hl_renderer.highlight("", source, "iso-8859-1", true)

    //Replace sourcecode section with highlighted source
    doc_xml = doc_xml.replace("<div class=\"sourcecode\">" + match[1] + "</div>", "<div class=\"sourcecode\">\n\n" + hl_source_fragment + "\n\n</div>")
}

builder     = DocumentBuilderFactory.newInstance().newDocumentBuilder()
inputStream = new ByteArrayInputStream(doc_xml.getBytes("UTF-8"))
org.w3c.dom.Document doc = builder.parse(inputStream)
inputStream.close()

renderer = new ITextRenderer()

//Add font variants for embedding
renderer.getFontResolver().addFont("gara.ttf", true);
renderer.getFontResolver().addFont("garait.ttf", true);
renderer.getFontResolver().addFont("garabd.ttf", true);
renderer.getFontResolver().addFont("trebuc.ttf", true);
renderer.getFontResolver().addFont("trebucit.ttf", true);
renderer.getFontResolver().addFont("trebucbi.ttf", true);
renderer.getFontResolver().addFont("trebucbd.ttf", true);

renderer.setDocument(doc, null)
renderer.layout()
os = new FileOutputStream(pdf_filename)
renderer.createPDF(os)
os.close()
