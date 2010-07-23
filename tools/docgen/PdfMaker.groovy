package docgen

import org.w3c.dom.Document
import org.xhtmlrenderer.pdf.ITextRenderer


class PdfMaker {

    List fonts

    PdfMaker(List fonts=[]) { this.fonts = fonts }

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

    static createRenderer(fonts=[]) {
        def renderer = new ITextRenderer()
        fonts.each {
            assert new File(it).exists(), "Font file <${it}> is not present."
            renderer.fontResolver.addFont(it, true)
        }
        return renderer
    }

}
