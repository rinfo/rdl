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

import net.sf.json.groovy.JsonSlurper

import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.ViewHandler
import se.lagrummet.rinfo.service.dataview.BasicViewHandler
//import se.lagrummet.rinfo.service.dataview.ModelViewHandler
import se.lagrummet.rinfo.service.dataview.RDataViewHandler


class SparqlTreeRouter extends Router {

    SparqlTreeRouter(Context context, Repository repository) {
        super(context)
        setDefaultMatchingMode(Template.MODE_EQUALS)

        def templates = new StringTemplateGroup("sparqltrees")
        templates.setFileCharEncoding("utf-8")
        templates.setRefreshInterval(0) // TODO: cache-control; make configurable

        // TODO: app-specific global data
        // .. reasonably updated by certain incoming resources (e.g. model, datasets)
        Map appData = [
            :
        ]
        // new RInfoServiceApplicationData()
        //appData.model.labels, appData.categories, ...

        //attachDefault(...)

        attach("/org",
                new SparqlTreeFinder(context,
                        new SparqlTreeViewer(repository, templates,
                                "sparqltrees/org/org-tree-rq",
                                "sparqltrees/org/org-html")
                    ) {
                    ViewHandler createViewHandler(String locale, Request request) {
                        new BasicViewHandler(locale, [encoding: "utf-8"])
                    }
                })
        //attach("/org/{path}",

        def searchFinder = new RDataSearchFinder(context,
                new SparqlTreeViewer(repository, templates,
                        "sparqltrees/rdata/rpubl-tree-rq",
                        "sparqltrees/rdata/publ_list_html"),
                appData)
        // TODO: even "/rpubl/-/{path}" goes to "/rpubl/{path}"!
        attach("/list/", searchFinder)
        attach("/list/-/{path}", searchFinder).getTemplate().getVariables().
                put("path", new Variable(Variable.TYPE_URI_PATH))

        def docFinder = new RDataDocFinder(context,
                new SparqlTreeViewer(repository, templates,
                        "sparqltrees/rdata/rpubl-tree-rq",
                        "sparqltrees/rdata/publ_doc_html"),
                appData)
        attach("/publ/{path}", docFinder).getTemplate().getVariables().
                put("path", new Variable(Variable.TYPE_URI_PATH))

        // TODO: find a nicer way to capture rest of path.. {path:anyPath}

        /* NOTE: unused (remove)
        def labelTree = new JsonSlurper().parse(
                ConfigurationUtils.locate("sparqltrees/model/model-settings.json"))
        attach("/model",
                new SparqlTreeFinder(context,
                            new SparqlTreeViewer(repository, templates,
                                    "sparqltrees/model/model-tree-rq",
                                    "sparqltrees/model/model-html")) {
                    ViewHandler createViewHandler(String locale, Request request) {
                        new ModelViewHandler(locale, null, labelTree)
                    }
                })
        */
    }

}


abstract class SparqlTreeFinder extends Finder {

    SparqlTreeViewer rqViewer
    static final DEFAULT_LOCALE = "sv"

    SparqlTreeFinder(Context context, SparqlTreeViewer rqViewer) {
        super(context)
        this.rqViewer = rqViewer
    }

    @Override
    Handler findTarget(Request request, Response response) {
        def resource = new Resource(getContext(), request, response)
        resource.variants.add(generateRepresentation(request))
        return resource
    }

    Representation generateRepresentation(Request request) {
        def locale = computeLocale(request)
        return new StringRepresentation(
                rqViewer.execute(createViewHandler(locale, request)),
                MediaType.TEXT_HTML,
                new Language(locale),
                new CharacterSet("utf-8"))
    }

    String computeLocale(Request request) {
        return DEFAULT_LOCALE // TODO: from path or conneg?
    }

    abstract ViewHandler createViewHandler(String locale, Request request);
}


abstract class RDataFinder extends SparqlTreeFinder {

    Map appData
    RDataFinder(Context context, SparqlTreeViewer rqViewer, Map appData) {
        super(context, rqViewer)
        this.appData = appData
    }
    ViewHandler createViewHandler(String locale, Request request) {
        return new RDataViewHandler(locale, appData, makeQueryData(request))
    }

    Map makeQueryData(Request request) {
        def path = request.attributes["path"]
        return queryDataFromPath(path)
    }

    abstract Map queryDataFromPath(String path);

}


class RDataSearchFinder extends RDataFinder {

    RDataSearchFinder(Context context, SparqlTreeViewer rqViewer, Map appData) {
        super(context, rqViewer, appData)
    }

    static final DEFAULT_MAX_ITEMS = 100

    Map queryDataFromPath(String path) {
        return [
            filter: createSearchFilter(path),
            get_relrev: false
        ]
    }

    protected String createSearchFilter(String path) {
        // TODO: create data to use in sparql stringtemplate instead!
        def categoryTokens = path? path.split("/"): []
        def filterParts = []
        for (token in categoryTokens) {
            if (token.indexOf("-") > -1) {
                def bits = token.split("-")
                if (bits[1] =~ /^\d+$/) {
                    filterParts.add('REGEX(STR(?doc__daterel), "[#/]'+bits[0]+'$")')
                    filterParts.add('REGEX(STR(?doc__daterel__value), "^'+bits[1]+'")')
                }
            } else {
                filterParts.add('REGEX(STR(?doc__1_type), "[#/]'+token+'")')
            }
        }
        if (!filterParts)
            return ""
        else
            return "FILTER(" + filterParts.join(" && ") + ")"
    }

}


class RDataDocFinder extends RDataFinder {

    RDataDocFinder(Context context, SparqlTreeViewer rqViewer, Map appData) {
        super(context, rqViewer, appData)
    }

    Map queryDataFromPath(String path) {
        // TODO: create data to use in sparql stringtemplate instead!
        def rinfoUri = "http://rinfo.lagrummet.se/publ/${path}"
        def filter = "FILTER(?doc = <${rinfoUri}>)"
        return [
            filter:filter,
            get_relrev: true
        ]
    }
}


