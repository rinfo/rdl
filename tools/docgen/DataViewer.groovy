package docgen

import org.w3c.dom.Document

import org.apache.commons.configuration.ConfigurationUtils
import net.sf.json.groovy.JsonSlurper
import org.antlr.stringtemplate.StringTemplateGroup

import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.ModelViewHandler


class DataViewer {
    def repo

    DataViewer(String... dirs) {
        repo = RDFUtil.slurpRdf(dirs)
    }

    def renderModel(fname=null, locale="en", mediabase=".") {
        def templates = new StringTemplateGroup("sparqltrees")
        def rqViewer = new SparqlTreeViewer(repo, templates,
                "sparqltrees/model/model-tree-rq", "sparqltrees/model/model-html")
        def labelTree = new JsonSlurper().parse(ConfigurationUtils.locate(
                        "sparqltrees/model/model-settings.json"))
        def options = [
            mediabase: mediabase
        ]
        def s = rqViewer.execute(new ModelViewHandler(locale, null, labelTree, options))
        if (fname) {
            println "Writing to: $fname"
            new File(fname).text = s
        } else {
            println s
        }
        return this
    }

}
