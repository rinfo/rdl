import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder
import se.lagrummet.rinfo.base.rdf.sparqltree.Lens
import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer


class ModelViewer extends SparqlTreeViewer {

    Map labels

    ModelViewer(labels, repo, templatePath, templates) {
        super(repo, templatePath, templates)
        this.labels = labels
    }

    Map queryToTree(String query) {
        def tree = super.queryToTree(query)
        tree.remove('someProperty')
        return tree;
    }

    Map queryToGraph(String query, Lens lens) {
        def labels = GraphBuilder.buildGraph(lens, labels)
        def data = new ModelData(super.queryToGraph(query, lens), labels)
        return [encoding: "utf-8",
                labels: data.labels,
                ontologies: data.ontologies]
    }

}
