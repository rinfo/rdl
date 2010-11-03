package se.lagrummet.rinfo.base.rdf;

import java.io.*;

import javax.xml.transform.Templates;
import org.openrdf.repository.Repository;
import se.lagrummet.rinfo.base.TransformerUtil;


public class GritTransformer {

    Templates gritXslt;
    Templates xslt;

    public GritTransformer(InputStream inputStream) {
        try {
            gritXslt = TransformerUtil.newTemplates(
                    getClass().getResourceAsStream("/xslt/rdfxml-grit.xslt"));
            xslt = TransformerUtil.newTemplates(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeXhtml(InputStream inputStream, Writer writer)
            throws IOException {
        TransformerUtil.writeXhtml(inputStream, writer, gritXslt, xslt);
    }

}
