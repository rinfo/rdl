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

    static final FILTER_TOKEN = "#FILTERS#"
    static final DEFAULT_MAX_ITEMS = 100

    RDataFinder(Context context, Repository repository, File treeDir) {
        super(context, repository,
                new File(treeDir, "rdata/rpubl-rqtree.xml"),
                new File(treeDir, "rdata/rpubl_to_rdata.xslt"),
                MediaType.APPLICATION_XML) // APPLICATION_ATOM_XML
    }

    String prepareQuery(Request request, String query) {
        def path = request.attributes["path"]
        def filter = ""

        if (!path || path.startsWith("-/")) { // prepare query
            // FIXME: either make different queries, or modularize it somehow instead!
            def strip = false
            def sb = new StringBuffer()
            for (l in query.split("\n")) {
                if (l.trim() == "#END relRevs#") {
                    strip = false; continue
                }
                else if (l.trim() == "#BEGIN relRevs#") {
                    strip = true; continue
                }
                if (strip) { continue }
                sb.append(l + "\n")
            }
            query = sb.toString()
            filter = createSearchFilter(path)

        } else { // expect entry uri
            def rinfoUri = "http://rinfo.lagrummet.se/${path}"
            filter = "FILTER(?subject = <${rinfoUri}>)"
        }

        return query.replace(FILTER_TOKEN, filter)
    }

    static String createSearchFilter(String path) {
        def categoryTokens = path.replace("-/", "").split("/")
        def filterParts = []
        for (token in categoryTokens) {
            if (token.indexOf("-") > -1) {
                def bits = token.split("-")
                if (bits[1] =~ /^\d+$/) {
                    filterParts.add('REGEX(STR(?dateRel), "[#/]'+bits[0]+'$")')
                    filterParts.add('REGEX(STR(?dateValue), "^'+bits[1]+'")')
                }
            } else {
                filterParts.add('REGEX(STR(?type), "[#/]'+token+'")')
            }
        }
        return (filterParts.size() == 0)? "" : "FILTER(" + filterParts.join(" && ") + ")"
    }

}
