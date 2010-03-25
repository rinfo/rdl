package se.lagrummet.rinfo.service


import org.restlet.Context
import org.restlet.data.CharacterSet
import org.restlet.data.Language
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status
import org.restlet.resource./*.representation.*/InputRepresentation
import org.restlet.resource./*.representation.*/StringRepresentation
import org.restlet.resource./*.representation.*/Representation
import org.restlet.resource./*.representation.*/Variant
import org.restlet./*resource.*/Finder
import org.restlet./*resource.*/Handler
import org.restlet.resource.Resource
import org.restlet./*routing.*/Router
import org.restlet.util.Template
import org.restlet.util.Variable

import javax.xml.transform.Templates
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.apache.commons.configuration.ConfigurationUtils

import org.openrdf.repository.Repository

import org.antlr.stringtemplate.StringTemplateGroup

import net.sf.json.JSONSerializer
//import net.sf.json.groovy.JsonSlurper

import se.lagrummet.rinfo.service.dataview.TemplateUtil
import se.lagrummet.rinfo.service.dataview.RDataSparqlTree


class SparqlTreeRouter extends Router {

    SparqlTreeRouter(Context context, Repository repo) {
        super(context)

        def templates = new StringTemplateGroup("sparqltrees")
        templates.setFileCharEncoding("utf-8")
        templates.setRefreshInterval(0) // TODO: cache-control; make configurable
        def tpltUtil = new TemplateUtil(templates)

        // TODO: app-specific global data
        // .. reasonably updated by certain incoming resources (e.g. model, datasets)
        Map appData = [
            "encoding": "utf-8",
            "resourceBaseUrl": "http://rinfo.lagrummet.se/",
            "basePath": "/rdata",
            "profile": [
                "prefix": [
                    "rpubl": "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#",
                    "dct": "http://purl.org/dc/terms/",
                ],
                "default": "rpubl",
                "define": ["a": "rdf:type"],
                "define": ["publisher": "dct:publisher"],
            ]
        ]

        // complete list
        attach("/org", new RDataFinder(context,
                    repo, appData, tpltUtil,
                    "sparqltrees/org/org-tree-rq",
                    "sparqltrees/org/org_html"))

        // TODO:IMPROVE:
        // - nicer paths? But even "/rpubl/-/{path}" goes to "/rpubl/{path}"!
        // - find a nicer way to capture path param.. Like {path:anyPath}..

        // filtering + narrowing
        attach("/browse/publ", new RDataFinder(context,
                    repo, appData, tpltUtil,
                    "sparqltrees/rdata/publ-list_all_params-rq",
                    "sparqltrees/rdata/publ_params_html"))
        //"/years/publ?"

        // list of
        attach("/list/publ", new RDataFinder(context,
                    repo, appData, tpltUtil,
                    "sparqltrees/rdata/rpubl-tree-rq",
                    "sparqltrees/rdata/publ_list_html"))

        // one
        attach("/publ/{path}", new RDataFinder(context,
                        repo, appData, tpltUtil,
                        "sparqltrees/rdata/rpubl-tree-rq",
                        "sparqltrees/rdata/publ_doc_html") {

                    @Override
                    Map createQueryData(Map httpQueryMap, String path) {
                        def queryData = super.createQueryData(httpQueryMap, path)
                        queryData['details'] = true
                        return queryData
                    }

            }).getTemplate().getVariables().
                put("path", new Variable(Variable.TYPE_URI_PATH))

    }

}


class RDataFinder extends Finder {

    static final DEFAULT_LOCALE = "sv"
    static final DEFAULT_MAX_LIMIT = 4096

    Repository repo
    Map appData
    TemplateUtil tpltUtil
    String queryTpltPath
    String viewTpltPath

    RDataFinder(Context context, Repository repo,
            appData, tpltUtil, queryTpltPath, viewTpltPath) {
        super(context)
        this.repo = repo
        this.appData = appData
        this.tpltUtil = tpltUtil
        this.queryTpltPath = queryTpltPath
        this.viewTpltPath = viewTpltPath
    }

    @Override
    Handler findTarget(Request request, Response response) {
        def repr = generateRepresentation(request)
        if (repr == null) {
            // TODO: 404..
            //response.setStatus(Status.CLIENT_ERROR_NOT_FOUND)
            return null
        }
        def resource = new Resource(getContext(), request, response)
        resource.variants.add(repr)
        return resource
    }

    Representation generateRepresentation(Request request) {
        def locale = computeLocale(request)
        def dataRqTree = new RDataSparqlTree(appData.resourceBaseUrl, locale)

        def queryData = getQueryData(request)
        def query = tpltUtil.runTemplate(queryTpltPath, queryData)
        def tree = dataRqTree.runQuery(repo, query)
        if (queryData["path"] && !tree.any { k, v -> v } )
            return null

        def asJson = request.resourceRef.extensions == "json"
        if (asJson) {
            return toRepresentation(JSONSerializer.toJSON(tree).toString(4),
                    MediaType.APPLICATION_JSON, locale)
        } else {
            tree["query"] = query
            tree.putAll(appData)
            return toRepresentation(tpltUtil.runTemplate(viewTpltPath, tree),
                    MediaType.TEXT_HTML, locale)
        }
    }

    String computeLocale(Request request) {
        return DEFAULT_LOCALE
    }

    def getQueryData(Request request) {
        // TODO: remove extension from path (always?)
        def path = request.attributes["path"]
        def httpQueryMap = request.resourceRef.queryAsForm.valuesMap
        return createQueryData(httpQueryMap, path)
    }

    Map createQueryData(Map httpQueryMap, String path) {
        def queryData = [
            "max_limit": DEFAULT_MAX_LIMIT,
        ]
        if (path) {
            queryData["path"] = path
        }
        def filters = []
        httpQueryMap?.each { key, value ->
            if (key == "a") {
                filters << ["typeSelector": value]
            } else {
                def selector = (value =~ /^\d+$/)? "dateSelector" : "leafSelector"
                filters << [(selector): key, value: value]
            }
        }
        if (filters) {
            queryData["filter_parts"] = filters
        }
        return queryData
    }

    def toRepresentation(repr, mediaType, locale) {
        return new StringRepresentation(repr, mediaType,
                new Language(locale), new CharacterSet("utf-8"))
    }

}
