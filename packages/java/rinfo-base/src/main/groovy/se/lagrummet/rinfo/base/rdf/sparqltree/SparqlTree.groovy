package se.lagrummet.rinfo.base.rdf.sparqltree

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.model.BNode
import org.openrdf.model.Literal
import org.openrdf.model.URI as RdfURI
import org.openrdf.model.Value
import org.openrdf.model.vocabulary.XMLSchema

import org.openrdf.query.MalformedQueryException
import org.openrdf.query.QueryEvaluationException
import org.openrdf.query.QueryLanguage
import org.openrdf.query.TupleQuery
import org.openrdf.query.TupleQueryResult
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.RepositoryException


class SparqlTree {

    private static final Logger logger = LoggerFactory.getLogger(SparqlTree.class)

    static URI_KEY = '$uri'
    static BNODE_KEY = '$id'
    static DATATYPE_KEY = '$datatype'
    static VALUE_KEY = '$value'
    static LANG_TAG = '@'
    static SEP = '__'
    static ONE_MARKER = '1_'

    Repository repo
    String query

    def varModel // stateful during debugging; refactor to VarTreeModel class

    SparqlTree(Repository repo, String query) {
        this.repo = repo
        this.query = query
    }

    Map runQuery() {
        def root = [:] // TODO: extended or turn to graph in-place? pojo+setProp?
        return runQuery(root)
    }

    Map runQuery(Map root) {
        def conn = repo.getConnection()
        def tree
        try {
            def tupleQuery = conn.prepareTupleQuery(
                    QueryLanguage.SPARQL, query)
            // TODO: configurable?
            tupleQuery.setIncludeInferred(false)
            def result = tupleQuery.evaluate() // TODO: or treeBuilder as resulthandler?
            tree = buildTree(result, root)
            result.close()
        } catch (IOException e) {
            throw new RuntimeException("Internal stream error.", e)
        } finally {
            conn.close()
        }
        return tree
    }

    Map buildTree(TupleQueryResult result, Map root) {
        logger.debug("Building tree..")
        varModel = makeVarTreeModel(result.getBindingNames())
        fillNodes(varModel, root, new ResultIterator(result))
        logger.debug("Tree done.")
        return root
    }

    Map makeVarTreeModel(List bindingNames) {
        bindingNames = new ArrayList(bindingNames)
        Collections.sort(bindingNames)
        def varTree = [:]
        for (var in bindingNames) {
            def currTree = varTree
            for (key in var.split(SEP)) {
                boolean useOne = false
                if (key.startsWith(ONE_MARKER)) {
                    useOne = true
                    key = key.substring(2, key.size())
                }
                if (currTree.containsKey(key)) {
                    currTree = currTree[key][-1]
                } else {
                    def subTree = [:]
                    def modelNode = [useOne, var, subTree]
                    currTree.put(key, modelNode)
                    currTree = subTree
                }
            }
        }
        return varTree
    }

    void fillNodes(Map varModel, Map treeNode, Iterable bindings) {
        varModel.entrySet().each { mapEntry ->
            def key = mapEntry.key
            def (useOne, varName, subVarModel) = mapEntry.value
            List nodes = []
            // FIXME: must pick more values from same bindingSet!
            // TODO: .. while no or cur-var-binding: get var+value+recurse; next..

            for (Map.Entry<String, List> resultMapEntry : groupBy(bindings,
                    { it.getValue(varName) }).entrySet()) {
                def varValue = resultMapEntry.getKey()
                def groupedBindings = resultMapEntry.getValue()

                if (!varValue) {
                    continue
                }
                def node = makeNode(varValue)

                /* TODO: ...
                // if already stored (less necessary if grouping is smart?
                // Nah, lack of REDUCED/DISTINCT reasonably requires this..)
                if (
                    any(n for n in nodes if n == node or
                        isinstance(node, dict) and isinstance(n, dict) and
                        [n.get(k) for k in node] == node.values())
                ) {
                    continue
                }
                */
                nodes.add(node)

                if (node instanceof Map && subVarModel) {
                    fillNodes(subVarModel, node, groupedBindings)
                }
            }
            treeNode[key] = useOne? oneify(nodes) : nodes
        }
    }

    Object makeNode(Value value) {
        def node = [:]
        def rawValue = value.stringValue()
        if (value instanceof RdfURI) {
            node[URI_KEY] = rawValue
        } else if (value instanceof BNode) {
            node[BNODE_KEY] = rawValue
        } else if (value instanceof Literal) {
            def lang = value.getLanguage()
            def datatype = value.getDatatype()
            if (lang != null) {
                node[LANG_TAG+lang] = rawValue
            } else if (datatype != null) {
                node = typeCast(datatype, value)
            } else {
                node = rawValue
            }
        } else {
            throw new Exception("TypeError: unknown value type for: " + value)
        }
        return node
    }

    /**
     * Casting the value (only if it is JSON compatible and can be
     * deterministically converted back).
     */
    Object typeCast(RdfURI datatype, Value value) {
        if (datatype == XMLSchema.BOOLEAN) {
            return value.booleanValue()
        } else if (datatype == XMLSchema.INTEGER) {
            return value.intValue()
        } else if (datatype == XMLSchema.FLOAT) {
            return value.floatValue()
        } else {
            def node = [:]
            node[VALUE_KEY] = value.stringValue()
            node[DATATYPE_KEY] = datatype
            return node
        }
    }

    Object oneify(nodes) {
        if (nodes.size() == 0) {
            return null
        }
        def first = nodes[0]
        if (isLangMap(first)) {
            first = new HashMap()
            for (node in nodes) {
                if (!(node instanceof Map)) {
                    node = [(LANG_TAG): node]
                }
                if (node.size() == 0) {
                    continue
                    // TODO: warn if a node isn't a dict:
                    //   "value was expected to be a lang dict but was %r."
                }
                // NOTE: lists of lang nodes only have 1 entry (lang+value)
                def item = node.entrySet().toArray()[0]
                first[item.key] = item.value
            }
        }
        /* TODO:
        else if (!laxOne and nodes.size() > 1) {
            throw new CardinalityException(nodes)
        }
        */
        return first
    }

    static boolean isLangMap(Object obj) {
        if (obj instanceof Map) {
            for (key in ((Map)obj).keySet()) {
                if (key.startsWith(LANG_TAG)) {
                    return true
                }
            }
        }
        return false
    }

    static Map groupBy(iterable, Closure keyfunc) {
        return iterable.inject([:]) { map, o ->
            def key = keyfunc(o)
            if (map.containsKey(key))
                map[key].add(o)
            else
                map[key] = [o]
            return map
        }
    }

    static boolean isLangNode(obj) {
        return (obj instanceof Map) && obj.find(
                { k, v -> k.startsWith(LANG_TAG) }) != null
    }

    static boolean isDatatypeNode(obj) {
        return (obj instanceof Map) && obj.containsKey(DATATYPE_KEY)
    }

    static boolean isLiteral(obj) {
        return !(obj instanceof Map) || isDatatypeNode(obj) || isLangNode(obj)
    }

    static boolean isResource(obj) {
        // FIXME: currently we do allow for "pure" anonymous nodes (w/o BNODE_KEY:s)
        // but this check is expensive(!):
        return ! isLiteral(obj)
    }



}


class ResultIterator implements Iterator, Iterable {
    TupleQueryResult tqResult
    ResultIterator(TupleQueryResult tqResult) {
        this.tqResult = tqResult
    }
    boolean hasNext() {
        return tqResult.hasNext()
    }
    Object next() {
        return tqResult.next()
    }
    void remove() {
        tqResult.remove()
    }
    Iterator iterator() {
        return this
    }
}
