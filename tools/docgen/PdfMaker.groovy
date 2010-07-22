package docgen

import org.w3c.dom.Document
import org.xhtmlrenderer.pdf.ITextRenderer


class PdfMaker {

    // "/Library/Fonts/Microsoft/Garamond"
    // "/Library/Fonts/Microsoft/Trebuchet MS"
    static DEFAULT_FONTS = [
            "gara.ttf", "garait.ttf", "garabd.ttf",
            "trebuc.ttf", "trebucit.ttf", "trebucbi.ttf", "trebucbd.ttf"
        ]
    List fonts
    PdfMaker() { fonts = DEFAULT_FONTS }
    PdfMaker(List fonts) { this.fonts = fonts }

    void renderAsPdf(Document doc, File outFile) {
        def outStream = new FileOutputStream(outFile)
        outFile.withOutputStream {
            renderAsPdf(doc, outFile.toURL().toString(), it)
        }
    }

    void renderAsPdf(Document doc, String docUrl, OutputStream outStream) {
        def renderer = createRenderer(fonts)
        renderer.setDocument(doc, docUrl)
        renderer.layout()
        renderer.createPDF(outStream)
    }

    static createRenderer(fonts=null) {
        def renderer = new ITextRenderer()
        if (fonts) {
            fonts.each {
                assert new File(it).exists(), 'Font file '+it+' not present'
                renderer.fontResolver.addFont(it, true)
            }
        }
        return renderer
    }

}
