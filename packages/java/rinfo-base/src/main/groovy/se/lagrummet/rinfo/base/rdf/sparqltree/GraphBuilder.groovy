package se.lagrummet.rinfo.base.rdf.sparqltree

class GraphBuilder {

    static Map buildGraph(Lens lens, Map tree) {
        return new GraphBuilder(lens).buildGraph(tree)
    }

    Lens lens
    protected Map<String, Map> index = new HashMap<String, Map>()

    GraphBuilder(Lens lens) {
        this.lens = lens
    }

    Map buildGraph(Map tree) {
        def graph = makeResource(tree, null)
        return graph
    }

     protected processNode(Object node, via) {
        if (SparqlTree.isResource(node)) {
            return makeResource(node, via)
        }
        else if (node instanceof List) {
            return castList(node, via)
        } else {
            return lens.castLiteral(node)
        }
     }

    protected makeResource(Map node, via) {
        def newres = lens.newResource(node)
        def resource = toIndexedResource(newres)
        if (via) {
            lens.updateVia(resource, via)
        }
        node.each { key, subnode ->
            resource[key] = processNode(subnode, [key, resource])
        }
        return resource
    }

     protected toIndexedResource(Map resource) {
        def indexed = getIndexedResource(resource)
        if (indexed == null) {
            indexResource(resource)
            return resource
        } else {
            lens.mergeResources(resource, indexed)
            return indexed
        }
     }

    Map getIndexedResource(Map resource) {
        return (index.get(resource[SparqlTree.URI_KEY]) ?:
                index.get("_:"+resource[SparqlTree.BNODE_KEY]))
    }

    void indexResource(Map resource) {
        if (resource.containsKey(SparqlTree.URI_KEY)) {
            index[resource[SparqlTree.URI_KEY]] = resource
        } else if (resource.containsKey(SparqlTree.BNODE_KEY)) {
            index["_:"+resource[SparqlTree.BNODE_KEY]] = resource
        }
    }

    List castList(List nodes, via) {
        return nodes.collect { processNode(it, via) }
    }

}
