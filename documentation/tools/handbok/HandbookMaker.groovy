import groovy.xml.dom.DOMUtil

import java.text.SimpleDateFormat


class HandbookMaker {

    static void main(String[] args) {
        if (args.length < 2) {
            println "Usage: %prog <input-url> <output-file> [overlay-file]"
            System.exit 1
        }
        def inUrl =  refToUrl(args[0])
        def outFile = new File(args[1])
        def tpltFile = (args.length == 3)? new File(args[2]) : null

        def doc = new DocTemplate(inUrl, tpltFile, [
                docdate: new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
                svnversion: getSvnVersionNumber()
            ]).render()

        if (outFile.name.endsWith("pdf")) {
            def out = new FileOutputStream(outFile)
            PdfMaker.renderAsPdf(doc, inUrl.toString(), out)
            out.close()
        } else {
            def out = outFile.name == "-" ? System.out : new FileOutputStream(outFile)
            DOMUtil.serialize(doc.documentElement, out)
        }
    }

    static refToUrl(String ref) {
        new URL(ref =~ /^https?:/ ? ref : "file:"+ref)
    }

    static getSvnVersionNumber() {
        def command = """svn info --xml"""
        def proc = command.execute()
        proc.waitFor()
        if (proc.exitValue() == 0) {
            def svnInfoDoc = new XmlParser().parseText(proc.in.text)
            return svnInfoDoc.entry.'@revision'.text()
        } else {
            return "0"
        }
    }

}
