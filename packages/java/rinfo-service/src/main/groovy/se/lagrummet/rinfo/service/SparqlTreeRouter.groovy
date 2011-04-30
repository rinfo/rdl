package se.lagrummet.rinfo.service


import org.restlet.Context
import org.restlet.data.CharacterSet
import org.restlet.data.Language
import org.restlet.data.MediaType
import org.restlet.data.Status
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.StringRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.Finder
import org.restlet.resource.Get
import org.restlet.resource.ServerResource
import org.restlet.routing.Router
import org.restlet.routing.Variable

import org.openrdf.repository.Repository

import org.antlr.stringtemplate.StringTemplateGroup

import net.sf.json.JSONSerializer
//import net.sf.json.groovy.JsonSlurper

import se.lagrummet.rinfo.service.dataview.TemplateUtil
import se.lagrummet.rinfo.service.dataview.RDataSparqlTree


class SparqlTreeRouter extends Router {

    static APP_DATA = [
        "encoding": "utf-8",
        "resourceBaseUrl": "http://rinfo.lagrummet.se/",
        "basePath": "/view",
        "profile": [
            "prefix": [
                "xsd": "http://www.w3.org/2001/XMLSchema#",
                "rpubl": "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#",
                "dct": "http://purl.org/dc/terms/",
            ],
            //"default": "rpubl",
            //"define": ["a": "rdf:type"],
            //"define": ["publisher": "dct:publisher"],
        ],
        "mediaTypes": [
            "application/rdf+xml": "RDF-data",
            "application/pdf": "PDF-dokument"
        ],
        "defaultSparqlLimit": 4096
    ]

    SparqlTreeRouter(Context context, Repository repo) {
        super(context)

        // TODO: app-specific global data
        // .. reasonably updated by certain incoming resources (e.g. model, datasets)
        Map appData = APP_DATA

        def templates = new StringTemplateGroup("sparqltrees")
        templates.setFileCharEncoding(appData["encoding"])
        templates.setRefreshInterval(0) // TODO: cache-control; make configurable
        def tpltUtil = new TemplateUtil(templates)

        attachDefault(new StringTemplateFinder(context, tpltUtil, appData,
                    "sparqltrees/index_html"))

        // complete list
        attach("/org", new RDataFinder(context, tpltUtil, appData, repo,
                    "sparqltrees/org/org_rq",
                    "sparqltrees/org/org_html"))

        // TODO:IMPROVE:
        // - Nicer paths than "browse","list?
        //   .. But even "/publ/-/{path}" goes to "/publ/{path}"!

        // filtering + narrowing
        def browseFinder = new RDataFinder(context, tpltUtil, appData, repo,
                "sparqltrees/publ/browse_rq",
                "sparqltrees/publ/browse_html") {

                @Override
                Map getQueryData(Request request) {
                    def queryData = [
                        "docType": request.attributes["type"],
                        "publisher": request.attributes["publisher"],
                    ]
                    return queryData
                }

            }
        attach("/browse/publ", browseFinder)
        attach("/browse/publ/{type}", browseFinder)
        attach("/browse/publ/{type}/{publisher}", browseFinder)
        //"/years/publ?"

        // list of
        attach("/list/publ/{type}/{publisher}/{dateProperty}@{year}",
            new RDataFinder(context, tpltUtil, appData, repo,
                "sparqltrees/publ/list_rq",
                "sparqltrees/publ/list_html") {

                @Override
                Map getQueryData(Request request) {
                    def queryData = [:]
                    def attrs = request.attributes
                    def filters = [
                        ["typeSelector": attrs["type"]],
                        ["dateSelector": attrs["dateProperty"], "value": attrs["year"]],
                        ["publisherSelector": true, "value": attrs["publisher"]],
                    ]
                    if (filters) {
                        queryData["filter_parts"] = filters
                    }
                    return queryData
                }

            })

        // one
        attach("/publ/{path}",
            new RDataFinder(context, tpltUtil, appData, repo,
                    "sparqltrees/publ/details_rq",
                    "sparqltrees/publ/doc_html") {

                @Override
                Map getQueryData(Request request) {
                    def path = request.attributes["path"]
                    if (path) {
                        def ext = request.resourceRef.extensions
                        if (ext && path.endsWith("."+ext)) {
                            path = path.substring(0, path.length()-(ext.length()+1))
                        }
                    }
                    return [ "path": path ]
                }

            }
        // TODO:IMPROVE: find a nicer way to capture path param.. Like {path:anyPath}..
        ).template.variables.put("path", new Variable(Variable.TYPE_URI_PATH))

    }

}


class StringTemplateFinder extends Finder {

    static final DEFAULT_LOCALE = "sv"

    TemplateUtil tpltUtil
    Map appData
    String viewTpltPath

    StringTemplateFinder(Context context, TemplateUtil tpltUtil,
            Map appData, String viewTpltPath) {
        super(context)
        this.appData = appData
        this.tpltUtil = tpltUtil
        this.viewTpltPath = viewTpltPath
    }

    String getLocale(clientInfo) {
        // TODO:? clientInfo.getPreferredLanguage(List<Language> supported).primaryTag
        return DEFAULT_LOCALE
    }

    Map makeViewData(String locale, Map data) {
        data.locale = locale
        data.putAll(appData)
        return data
    }

    String makeHtmlView(Map data) {
        return tpltUtil.runTemplate(viewTpltPath, data)
    }

    Representation toRepresentation(repr, mediaType, locale) {
        return new StringRepresentation(repr, mediaType,
                new Language(locale), new CharacterSet("utf-8"))
    }

    @Override
    ServerResource find(Request request, Response response) {
        def locale = getLocale(request.clientInfo)
        return new ServerResource() {
            @Get("html|xhtml")
            Representation asHtml() {
                def viewData = makeViewData(locale, [:])
                return toRepresentation(makeHtmlView(viewData),
                        MediaType.TEXT_HTML, locale)
            }
        }
    }

}


class RDataFinder extends StringTemplateFinder {

    Repository repo
    String queryTpltPath
    boolean devMode = true

    RDataFinder(Context context, TemplateUtil tpltUtil,
            Map appData, Repository repo,
            String queryTpltPath, String viewTpltPath) {
        super(context, tpltUtil, appData, viewTpltPath)
        this.repo = repo
        this.queryTpltPath = queryTpltPath
    }

    /**
     * Template method used to extract params to query from request.
     */
    Map getQueryData(Request request) {
        return [:]
    }

    @Override
    ServerResource find(Request request, Response response) {

        if (isDevMode() && request.resourceRef.queryAsForm.getFirst("query")) {
            def rq = makeQuery(getQueryData(request))
            return new ServerResource() {
                @Override
                Representation doHandle() {
                    return toRepresentation(rq, MediaType.TEXT_PLAIN, null)
                }
            }
        }

        // TODO:IMPROVE: in theory, the use of getQueryData is open for
        // "SPARQL injection". We "should" guard against it by properly
        // checking/escaping the values.
        def locale = getLocale(request.clientInfo)
        def data = runQuery(locale, getQueryData(request))

        return new ServerResource() {

            @Get("html|xhtml")
            Representation asHtml() {
                if (data == null) {
                    setStatus(Status.CLIENT_ERROR_NOT_FOUND)
                    return null
                }
                def viewData = makeViewData(locale, data)
                return toRepresentation(makeHtmlView(viewData),
                        MediaType.TEXT_HTML, locale)
            }

            @Get("json")
            Representation asJson() {
                if (data == null) {
                    setStatus(Status.CLIENT_ERROR_NOT_FOUND)
                    return null
                }
                return toRepresentation(JSONSerializer.toJSON(data).toString(4),
                        MediaType.APPLICATION_JSON, locale)
            }

        }

    }

    Map runQuery(String locale, Map queryData) {
        def query = makeQuery(queryData)
        def dataRqTree = new RDataSparqlTree(appData, locale)
        def data = dataRqTree.runQuery(repo, query)
        if (queryData["path"] && !data.any { k, v -> v } )
            return null
        return data
    }

    String makeQuery(Map queryData) {
        return tpltUtil.runTemplate(queryTpltPath, queryData)
    }

}
