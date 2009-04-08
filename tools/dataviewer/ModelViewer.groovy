import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder
import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer


class ModelViewer extends SparqlTreeViewer {

    Map labels

    ModelViewer(repo, query, lens, templatePath, jsonPath) {
        super(repo, query, lens, templatePath)
        this.labels = GraphBuilder.buildGraph(lens,
                toJSON(new File(jsonPath)))
    }

    Map queryToTree() {
        def tree = super.queryToTree()
        tree.remove('someProperty')
        return tree;
    }

    Map queryToGraph() {
        def data = new ModelData(super.queryToGraph(), labels)
        return [encoding: "utf-8",
                labels: data.labels,
                ontologies: data.ontologies]
    }

}
