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
import org.restlet.util.Variable

import javax.xml.transform.Templates
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.SparqlTree


class SparqlTreeRouter extends Router {

    SparqlTreeRouter(Context context, Repository repository, File treeDir) {
        super(context)

        attach("/model", new ModelFinder(context, repository, treeDir))

        // TODO: nice capture of rest of path.. {path:anyPath} (+ /entry?)
        def route = attach("/rdata/{path}", new RDataFinder(context, repository, treeDir))
        Map<String, Variable> routeVars = route.getTemplate().getVariables()
        routeVars.put("path", new Variable(Variable.TYPE_URI_PATH))

        /* TODO:...
        attach("/rdata/publ", new RDataSearchFinder(context, repository, treeDir))
        //? attach("search", new SmartOpenSearchFinder(...))
        */
    }

}


class SparqlTreeFinder extends Finder {

    SparqlTree rqTree
    Templates outputXslt
    MediaType mediaType

    SparqlTreeFinder() {}

    SparqlTreeFinder(Context context, Repository repository,
            File treeFile, File outXsltFile, MediaType mediaType) {
        super(context)
        rqTree = new SparqlTree(repository, treeFile)
        outputXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
                new StreamSource(outXsltFile))
        this.mediaType = mediaType
    }

    @Override
    Handler findTarget(Request request, Response response) {
        def resource = new Resource(getContext(), request, response)
        resource.variants.add(
                generateRepresentation(
                        prepareQuery(request, rqTree.queryString))
            )
        return resource
    }

    Representation generateRepresentation(String query) {
        def outStream = new ByteArrayOutputStream()
        rqTree.queryAndChainToResult(
                query, new StreamResult(outStream), outputXslt)
        return new InputRepresentation(
                new ByteArrayInputStream(outStream.toByteArray()),
                mediaType)
    }

    String prepareQuery(Request request, String query) {
        return query
    }

}

class ModelFinder extends SparqlTreeFinder {

    ModelFinder(Context context, Repository repository, File treeDir) {
        super(context, repository,
                new File(treeDir, "model/sparqltree-model.xml"),
                new File(treeDir, "model/modeltree_to_html.xslt"),
                MediaType.TEXT_HTML)
    }

}

class RDataFinder extends SparqlTreeFinder {

    RDataFinder(Context context, Repository repository, File treeDir) {
        super(context, repository,
                new File(treeDir, "rdata/rpubl-rqtree.xml"),
                new File(treeDir, "rdata/rpubl_to_rdata.xslt"),
                MediaType.APPLICATION_XML) // APPLICATION_ATOM_XML
    }

    String prepareQuery(Request request, String query) {
        def path = request.attributes["path"]
        def rinfoUri = "http://rinfo.lagrummet.se/${path}"
        def QUERY_URI_TOKEN = "http://rinfo.lagrummet.se/publ/sfs/1999:175"
        return query.replace(QUERY_URI_TOKEN, rinfoUri)
    }

}
