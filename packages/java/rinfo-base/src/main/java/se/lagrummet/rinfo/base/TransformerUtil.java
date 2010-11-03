package se.lagrummet.rinfo.base;

import java.io.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;


public class TransformerUtil {

    static SAXTransformerFactory saxTf =
            (SAXTransformerFactory) TransformerFactory.newInstance();

    public static Templates newTemplates(InputStream inputStream) throws Exception {
        return saxTf.newTemplates(new StreamSource(inputStream));
    }

    public static void writeXhtml(InputStream inputStream, Writer writer,
            Templates... xslts) throws IOException {
        try {
            XMLFilter filter = null;
            for (Templates xslt : xslts) {
                XMLFilter nextFilter = saxTf.newXMLFilter(xslt);
                if (filter != null) nextFilter.setParent(filter);
                filter = nextFilter;
            }
            Transformer htmlTransformer = saxTf.newTransformer();
            SAXSource transformSource = new SAXSource(filter, new InputSource(inputStream));
            htmlTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
            htmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            htmlTransformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
                    "-//W3C//DTD XHTML 1.0 Strict//EN");
            htmlTransformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
            htmlTransformer.transform(transformSource, new StreamResult(writer));
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

}
