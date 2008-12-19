package se.lagrummet.rinfo.service

import org.restlet.Context
import org.restlet.Finder
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

    SparqlTreeRouter(Context context, File treeDir) {
        super(context)

        // FIXME: spaghetti - setup repo in ServiceApplication and send to this ctor!
        def loadScheduler = (SesameLoadScheduler) context.attributes[
                ServiceApplication.RDF_LOADER_CONTEXT_KEY]
        def repo = loadScheduler.repository

        attach("/model", new ModelFinder(context, treeDir, repo))
        /* TODO:...
        attach("rdata/publ/{path:anyPath}", new RDataFinder(...))
        //? attach("search", new SmartOpenSearchFinder(...))
        */
    }

}

class ModelFinder extends Finder {

    SparqlTree rqTree
    Templates toHtmlXslt

    ModelFinder(Context context, File treeDir, Repository repo) {
        super(context)
        rqTree = new SparqlTree(repo, new File(treeDir, "sparqltree-model.xml"))
        toHtmlXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
                new StreamSource(new File(treeDir, "modeltree_to_html.xslt")))
    }

    @Override
    Handler findTarget(Request request, Response response) {
        def resource = new Resource(getContext(), request, response)
        resource.variants << newModelHtml()
        return resource
    }

    Representation newModelHtml() {
        def outStream = new ByteArrayOutputStream()
        rqTree.queryAndChainToResult(new StreamResult(outStream), toHtmlXslt)
        return new InputRepresentation(
                new ByteArrayInputStream(outStream.toByteArray()),
                MediaType.TEXT_HTML)
    }

}
