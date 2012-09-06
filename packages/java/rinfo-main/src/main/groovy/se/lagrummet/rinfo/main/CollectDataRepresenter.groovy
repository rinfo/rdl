
package se.lagrummet.rinfo.main

import org.restlet.data.MediaType
import org.restlet.representation.InputRepresentation
import org.restlet.representation.Representation
import org.restlet.representation.WriterRepresentation
import org.restlet.representation.Variant

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.GritTransformer
import se.lagrummet.rinfo.base.rdf.RDFUtil


class CollectDataRepresenter {

    GritTransformer logToXhtml

    def variants = [new Variant(MediaType.APPLICATION_RDF_XML),
                    new Variant(MediaType.TEXT_HTML)]

    CollectDataRepresenter(logToXhtml) {
        this.logToXhtml = logToXhtml
    }

    Representation represent(Repository repo, Variant variant, String context=null) {
        // TODO: return null if context is not found
        def ins = context != null?
            RDFUtil.toInputStream(repo, "application/rdf+xml", false,
                    repo.valueFactory.createURI(context)) :
            RDFUtil.toInputStream(repo, "application/rdf+xml", false)

        if (MediaType.APPLICATION_RDF_XML.equals(variant.getMediaType())) {
            return new InputRepresentation(ins, MediaType.APPLICATION_RDF_XML)

        } else if (MediaType.TEXT_HTML.equals(variant.getMediaType())) {
            return new WriterRepresentation(MediaType.TEXT_HTML) {
                public void write(Writer writer) throws IOException {
                    try {
                        logToXhtml.writeXhtml(ins, writer);
                    } finally {
                        ins.close();
                    }
                }
            }
        } else {
            return null
        }
    }

}
