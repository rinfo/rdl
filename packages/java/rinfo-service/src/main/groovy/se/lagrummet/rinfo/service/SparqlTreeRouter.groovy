package se.lagrummet.rinfo.service

import org.restlet.Context
import org.restlet.Finder
import org.restlet.Handler
import org.restlet.Router
import org.restlet.data.Language
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status
import org.restlet.resource.InputRepresentation
import org.restlet.resource.StringRepresentation
import org.restlet.resource.Representation
import org.restlet.resource.Resource
import org.restlet.resource.Variant
import org.restlet.util.Variable

import javax.xml.transform.Templates
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.apache.commons.configuration.ConfigurationUtils

import org.openrdf.repository.Repository

import org.antlr.stringtemplate.StringTemplateGroup

import net.sf.json.groovy.JsonSlurper

import se.lagrummet.rinfo.base.rdf.SparqlTree

import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.ModelResultHandler


class SparqlTreeRouter extends Router {

    SparqlTreeRouter(Context context, Repository repository) {
        super(context)

        def templates = new StringTemplateGroup("sparqltrees")
        templates.setRefreshInterval(0) // TODO: cache-control; make configurable

        attach("/org", new SparqlTreeFinder(context,
                    new SparqlTreeViewer(repository, templates,
                            "sparqltrees/org/org-tree-rq", "sparqltrees/org/org-html"),
                    MediaType.TEXT_HTML))

        def labelTree = new JsonSlurper().parse(
                ConfigurationUtils.locate("sparqltrees/model/model_labels.json"))
        attach("/model", new ModelFinder(context,
                    new SparqlTreeViewer(repository, templates,
                            "sparqltrees/model/model-tree-rq",
                            "sparqltrees/model/model_html"),
                    MediaType.TEXT_HTML))

        // TODO: nice capture of rest of path.. {path:anyPath} (+ /entry?)
        def route = attach("/rdata/{path}", new RDataFinder(context, repository))
        Map<String, Variable> routeVars = route.getTemplate().getVariables()
        routeVars.put("path", new Variable(Variable.TYPE_URI_PATH))
    }

}


class SparqlTreeFinder extends Finder {

    MediaType mediaType
    SparqlTreeViewer rqViewer
    String query

    SparqlTreeFinder(Context context,
            String queryPath,
            SparqlTreeViewer rqViewer,
            MediaType mediaType) {
        super(context)
        this.mediaType = mediaType
        this.rqViewer = rqViewer
        // TODO: query <= queryPath
    }

    @Override
    Handler findTarget(Request request, Response response) {
        def resource = new Resource(getContext(), request, response)
        resource.variants.add(
                generateRepresentation(request)
            )
        return resource
    }

    Representation generateRepresentation(Request request) {
        def locale = computeLocale(request)
        return new StringRepresentation(
                rqViewer.execute(createResultHandler(locale, request)),
                mediaType,
                new Language(locale))
    }

    String computeLocale(Request request) {
        return "sv" // FIXME: from path or conneg
    }

    String createResultHandler(String locale, Request request) {
        return query
    }

}


class LegacySparqlTreeFinder extends Finder {

    SparqlTree rqTree
    Templates outputXslt
    MediaType mediaType

    LegacySparqlTreeFinder(Context context, Repository repository,
            String treePath, String outputXsltPath, MediaType mediaType) {
        this(context, repository,
                locate(treePath), locate(outputXsltPath), mediaType)
    }

    LegacySparqlTreeFinder(Context context, Repository repository,
            URL treeUrl, URL outputXsltUrl, MediaType mediaType) {
        super(context)
        rqTree = new SparqlTree(repository, treeUrl)
        outputXslt = SparqlTree.TRANSFORMER_FACTORY.newTemplates(
                new StreamSource(outputXsltUrl.openStream()))
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

    protected static URL locate(String name) {
        return ConfigurationUtils.locate(name)
    }

}


class RDataFinder extends LegacySparqlTreeFinder {

    static final FILTER_TOKEN = "#FILTERS#"
    static final DEFAULT_MAX_ITEMS = 100

    RDataFinder(Context context, Repository repository) {
        super(context, repository,
                locate("sparqltrees/rdata/rpubl-rqtree.xml"),
                locate("sparqltrees/rdata/rpubl_to_rdata.xslt"),
                MediaType.APPLICATION_XML) // APPLICATION_ATOM_XML
    }

    String prepareQuery(Request request, String query) {
        def path = request.attributes["path"]
        def filter = ""

        if (!path || path.startsWith("-/")) { // prepare query
            /* TODO: either make different queries, or modularize it somehow instead!
                - <rdata_search-rqtree.xml>
                    - q in awol:title, awol:subtitle, awol:summary
                    - inserted category filters.. .. how?
                        .. ?rel endswith part1; ?value endswith part2
                        .. ?dateRel -||-; ?dateValue startswith part2
                        .. incl. narrower categories?
                - <rpubl_to_rdata_index-rqtree.xml>
                - <rdata_indexed_rpubl-rqtree.xml>
            */
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
