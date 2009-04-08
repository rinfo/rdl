package se.lagrummet.rinfo.service.dataview

import org.openrdf.repository.Repository

import org.antlr.stringtemplate.StringTemplate

import net.sf.json.JSON
import net.sf.json.groovy.JsonSlurper

import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder
import se.lagrummet.rinfo.base.rdf.sparqltree.Lens
import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree


class SparqlTreeViewer {

    Repository repo
    String query
    Lens lens
    String templatePath

    SparqlTreeViewer(repo, query, lens=null, templatePath=null) {
        this.repo = repo
        this.query = query
        this.lens = lens
        this.templatePath = templatePath
    }

    StringTemplate execute() {
        return execute(new HashMap())
    }

    StringTemplate execute(Map data) {
        def result = queryToResult()
        data.putAll(result)
        return runTemplate(result)
    }

    Map queryToResult() {
        return (lens != null)? queryToGraph() : queryToTree()
    }

    Map queryToTree() {
        return SparqlTree.runQuery(repo, query)
    }

    Map queryToGraph() {
        return GraphBuilder.buildGraph(lens, queryToTree())
    }

    StringTemplate runTemplate(Map data) {
        def st = new StringTemplate(new File(templatePath).text)
        data.each { key, value ->
            st.setAttribute(key, value)
        }
        return st
    }

    static JSON toJSON(source) {
        return new JsonSlurper().parse(source)
    }

}
