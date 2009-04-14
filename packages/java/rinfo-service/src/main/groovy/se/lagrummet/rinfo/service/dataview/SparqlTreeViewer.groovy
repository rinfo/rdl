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
    String viewPath

    SparqlTreeViewer(Repository repo, String queryPath, String viewPath) {
        this(repo, null, queryPath, viewPath)
    }

    SparqlTreeViewer(Repository repo, StringTemplateGroup templates,
            String queryPath, String viewPath) {
        this.repo = repo
        this.templates = templates
        this.queryPath = queryPath
        this.viewPath = viewPath
    }

    String execute(ViewHandler handler) {
        def query = runTemplate(queryPath, handler.getQueryData())
        def tree = handler.handleTree(SparqlTree.runQuery(repo, query))
        def result = handler.handleGraph(
                GraphBuilder.buildGraph(handler.getLens(), tree))
        return runTemplate(viewPath, result)
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
