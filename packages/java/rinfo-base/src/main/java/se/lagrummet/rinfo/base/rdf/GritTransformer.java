package se.lagrummet.rinfo.base.rdf;

import java.io.*;
import org.apache.commons.lang.ArrayUtils;

import javax.xml.transform.Templates;
import org.openrdf.repository.Repository;
import se.lagrummet.rinfo.base.TransformerUtil;


public class GritTransformer {

    Templates[] templates;

    public GritTransformer(Templates... templates) {
        try {
            Templates gritXslt = TransformerUtil.newTemplates(
                    getClass(), "/xslt/rdfxml-grit.xslt");
            this.templates = (Templates[]) ArrayUtils.add(templates, 0, gritXslt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeXhtml(InputStream inputStream, Writer writer)
            throws IOException {
        TransformerUtil.writeXhtml(inputStream, writer, templates);
    }

}
