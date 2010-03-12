package se.lagrummet.rinfo.service.dataview

import org.openrdf.repository.Repository

import org.antlr.stringtemplate.StringTemplate
import org.antlr.stringtemplate.StringTemplateGroup

import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder
import se.lagrummet.rinfo.base.rdf.sparqltree.Lens
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree


class SparqlTreeViewer {

    Repository repo
    StringTemplateGroup templates
    String queryPath
    String templatePath

    SparqlTreeViewer(Repository repo, String queryPath, String templatePath) {
        this(repo, null, queryPath, templatePath)
    }

    SparqlTreeViewer(Repository repo, StringTemplateGroup templates,
            String queryPath, String templatePath) {
        this.repo = repo
        this.templates = templates
        this.queryPath = queryPath
        this.templatePath = templatePath
    }

    String execute(ViewHandler handler) {
        def tree = execQuery(handler)
        def result = handler.handleGraph(
                GraphBuilder.buildGraph(handler.getLens(), tree))
        return runTemplate(templatePath, result)
    }

    Map execQuery(ViewHandler handler) {
        def query = runTemplate(queryPath, handler.getQueryData())
        // TODO: log
        //logger.debug("Query: " + "="*72 + query + "="*72)
        def result = handler.handleTree(SparqlTree.runQuery(repo, query))
        // FIXME: only for debug (to put query as comment in result html)
        result['query'] = query
        return result
    }

    protected String runTemplate(String templatePath, Map data) {
        def st = (templates != null)?
                templates.getInstanceOf(templatePath) :
                new StringTemplate(new File(templatePath).text)
        if (data != null) {
            data.each { key, value ->
                st.setAttribute(key, value)
            }
        }
        return st.toString()
    }

}
