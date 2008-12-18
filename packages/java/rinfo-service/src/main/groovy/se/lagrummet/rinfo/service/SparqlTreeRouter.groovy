package se.lagrummet.rinfo.service

import org.restlet.Context
import org.restlet.Handler
import org.restlet.Router
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status
import org.restlet.resource.InputRepresentation
import org.restlet.resource.Representation
import org.restlet.resource.Resource
import org.restlet.resource.Variant

import javax.xml.transform.Templates
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.SparqlTree


class SparqlTreeRouter extends Router {

    File treeDir

    public static final String SPARQL_TREE_CONTEXT_KEY =
            "rinfo.service.restlet.context.spareqltree.dir"

    SparqlTreeRouter(Context context, File treeDir) {
        super(context)
        this.treeDir = treeDir

        getContext().getAttributes().putIfAbsent(SPARQL_TREE_CONTEXT_KEY, treeDir)

        attach("/model", ModelResource)
        /* TODO:...
        attach("rdata/publ/{path:anyPath}", new RDataResource(...))
        //? attach("search", new SmartOpenSearchResource(...))
        */
    }

    @Override
    Handler findTarget(Request request, Response response) {
        return null
    }

}

class ModelResource extends Resource {

    SparqlTree rqTree
    Templates toHtmlXslt

    ModelResource(Context context, Request request, Response response) {
        super(context, request, response)

        def attrs = getContext().getAttributes()

        // FIXME: spaghetti - expose repo more generally!
        def loadScheduler = (SesameLoadScheduler) getContext().getAttributes().get(
                ServiceApplication.RDF_LOADER_CONTEXT_KEY)
        def repo = loadScheduler.getRepository()

        def treeDir = (File) attrs.get(SparqlTreeRouter.SPARQL_TREE_CONTEXT_KEY)

        rqTree = new SparqlTree(repo, new File(treeDir, "sparqltree-model.xml"))
        toHtmlXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
                new StreamSource(new File(treeDir, "modeltree_to_html.xslt")))
    }

    @Override
    public boolean isReadable() { return true; }

    @Override
    public void handleGet() {
        def outStream = new ByteArrayOutputStream()
        rqTree.queryAndChainToResult(new StreamResult(outStream), toHtmlXslt)
        getResponse().setEntity(
                new InputRepresentation(
                        new ByteArrayInputStream(outStream.toByteArray()),
                        MediaType.TEXT_HTML))
    }

}
