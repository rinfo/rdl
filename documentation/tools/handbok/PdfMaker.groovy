import org.w3c.dom.Document
import org.xhtmlrenderer.pdf.ITextRenderer


@Grab(group='com.lowagie', module='itext', version='2.0.8')
@Grab(group='org.xhtmlrenderer', module='core-renderer', version='R8pre2')
class PdfMaker {

    static DEFAULT_FONTS = [
            "gara.ttf", "garait.ttf", "garabd.ttf",
            "trebuc.ttf", "trebucit.ttf", "trebucbi.ttf", "trebucbd.ttf"
        ]

    static void renderAsPdf(Document doc, String docUrl, outStream) {
        def renderer = createRenderer(DEFAULT_FONTS)
        renderer.setDocument(doc, docUrl)
        renderer.layout()
        renderer.createPDF(outStream)
    }

    static createRenderer(fonts=null) {
        def renderer = new ITextRenderer()
        if (fonts) {
            fonts.each {
                if (new File(it).exists()) {
                    renderer.fontResolver.addFont(it, true)
                }
            }
        }
        return renderer
    }

}
