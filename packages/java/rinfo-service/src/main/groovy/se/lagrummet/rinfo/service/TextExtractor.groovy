package se.lagrummet.rinfo.service

import org.apache.tika.detect.DefaultDetector
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.html.BoilerpipeContentHandler


class TextExtractor {

    def detector
    def parser
    def context

    TextExtractor() {
        detector = new DefaultDetector()
        parser = new AutoDetectParser(detector)
        context = new ParseContext()
    }

    def getString(file) {
        def metadata = new Metadata()
        def output = new ByteArrayOutputStream()
        def input = TikaInputStream.get(file, metadata)
        try {
            def writer = new OutputStreamWriter(output, "UTF-8")
            def handler = new BoilerpipeContentHandler(writer)
            parser.parse(input, handler, metadata, context)
        } finally {
            input.close()
            return output.toString("UTF-8")
        }
    }

}
