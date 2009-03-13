
import org.xhtmlrenderer.pdf.ITextRenderer
import com.lowagie.text.DocumentException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import java.io.FileOutputStream
import java.io.File


pdf_filename = "Handbok-Myndighetsforeskrifter.pdf"
input_file = "myndighetsforeskrift.xhtml"
//doc_xml = new File(input_file).getText("UTF-8")

url = new File(input_file).toURI().toURL().toString()

builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
doc = builder.parse(url)

renderer = new ITextRenderer()

//Load fonts
renderer.getFontResolver().addFont("gara.ttf", true);
renderer.getFontResolver().addFont("trebuc.ttf", true);

renderer.setDocument(doc, null)
renderer.layout()
os = new FileOutputStream(pdf_filename)
renderer.createPDF(os)
os.close()
