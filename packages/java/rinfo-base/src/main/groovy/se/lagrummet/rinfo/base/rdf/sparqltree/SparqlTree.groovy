package se.lagrummet.rinfo.base.rdf.sparqltree

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.openrdf.model.BNode
import org.openrdf.model.Literal
import org.openrdf.model.URI
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

    static String SEP = '__'
    static String ONE_MARKER = '1_'

    static String DEFAULT_URI_KEY = '$uri'
    static String DEFAULT_BNODE_KEY = '$id'
    static String DEFAULT_DATATYPE_KEY = '$datatype'
    static String DEFAULT_VALUE_KEY = '$value'
    static String DEFAULT_LANG_TAG = '@'

    String uriKey = DEFAULT_URI_KEY
    String bnodeKey = DEFAULT_BNODE_KEY
    String datatypeKey = DEFAULT_DATATYPE_KEY
    String valueKey = DEFAULT_VALUE_KEY
    String langTag = DEFAULT_LANG_TAG
    //String locale = null
    //boolean laxOne = true

    /**
     * A SparqlTree instance is stateless apart from its configuration
     * properties. It is thus thread-safe and ok to keep around indefinitely.
     */
    SparqlTree() {
    }

    Map runQuery(Repository repo, String query) {
        def root = [:]
        def conn = repo.getConnection()
        def resultTree
        try {
            TupleQuery tupleQuery = conn.prepareTupleQuery(
                    QueryLanguage.SPARQL, query)
            // TODO: configurable..
            tupleQuery.setIncludeInferred(false)
            logger.debug("Running query..")
            TupleQueryResult result = tupleQuery.evaluate()
            logger.debug("Query complete.")
            try {
                resultTree = buildTree(new QueryResult(result), root)
            } finally {
                result.close()
            }
        } catch (IOException e) {
            throw new RuntimeException("Internal stream error.", e)
        } finally {
            conn.close()
        }
        return resultTree
    }

    Map buildTree(QueryResult result, Map root) {
        logger.debug("Building tree..")
        def varModel = makeVarTreeModel(result.getBindingNames())
        fillNodes(varModel, root, result)
        logger.debug("Tree done.")
        return root
    }

    boolean isLangNode(obj) {
        return (obj instanceof Map) && obj.find(
                { k, v -> k.startsWith(langTag) }) != null
    }

    boolean isDatatypeNode(obj) {
        return (obj instanceof Map) && obj.containsKey(datatypeKey)
    }

    boolean isLiteral(obj) {
        return !(obj instanceof Map) || isDatatypeNode(obj) || isLangNode(obj)
    }

    boolean isResource(obj) {
        // TODO: currently we do allow for "pure" anonymous nodes (w/o BNODE_KEY:s)
        // but this check is more expensive:
        return ! isLiteral(obj)
    }

    /**
     * Overridable template method which is called with a node once it has been
     * populated with all keys and descendant values.
     *
     * @param node The value this node will be represented by, unless altered
     *             by this method call.
     * @param parentNode The parent node where this node will be put.
     * @param key The name for which this node is reached from the parentNode.
     * @return The final value for the node (defaults to the unmodified node
     *         itself).
     */
    Object completeNode(Object node, String key, Map parentNode) {
        return node
    }

    protected Map makeVarTreeModel(List bindingNames) {
        bindingNames = new ArrayList(bindingNames)
        Collections.sort(bindingNames)
        def varTree = [:]
        for (var in bindingNames) {
            if (var.startsWith("_"))
                continue
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

    protected void fillNodes(Map varModel, Map parentNode, Iterable bindings) {
        for (mapEntry in varModel.entrySet()) {
            def key = mapEntry.key
            def (useOne, varName, subVarModel) = mapEntry.value
            List<Object> nodes = []

            for (Map.Entry<String, List> resultMapEntry : groupBy(bindings,
                    { it.getValue(varName) }).entrySet()) {
                def varValue = resultMapEntry.getKey()
                if (!varValue) {
                    continue
                }

                def groupedBindings = resultMapEntry.getValue()
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
            def finalValue = nodes
            if (useOne) {
                finalValue = completeNode(toOne(nodes), key, parentNode)
            } else {
                for (int i = 0; i < nodes.size(); i++) {
                    nodes[i] = completeNode(nodes[i], key, parentNode)
                }
            }
            parentNode[key] = finalValue
        }
    }

    protected Object makeNode(Value value) {
        def node = [:]
        def stringValue = value.stringValue()
        if (value instanceof org.openrdf.model.URI) {
            node[uriKey] = stringValue
        } else if (value instanceof BNode) {
            node[bnodeKey] = stringValue
        } else if (value instanceof Literal) {
            def lang = value.getLanguage()
            def datatype = value.getDatatype()
            if (lang != null) {
                node[langTag+lang] = stringValue
            } else if (datatype != null) {
                node = typeCast(datatype, value)
            } else {
                node = stringValue
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
    protected Object typeCast(org.openrdf.model.URI datatype, Value value) {
        if (datatype == XMLSchema.BOOLEAN) {
            return value.booleanValue()
        } else if (datatype == XMLSchema.INTEGER) {
            return value.intValue()
        } else if (datatype == XMLSchema.FLOAT) {
            return value.floatValue()
        } else {
            def node = [:]
            node[valueKey] = value.stringValue()
            node[datatypeKey] = datatype.toString()
            return node
        }
    }

    protected Object toOne(nodes) {
        if (nodes.size() == 0) {
            return null
        }
        def first = nodes[0]
        if (isLangMap(first)) {
            first = new HashMap()
            for (node in nodes) {
                if (!(node instanceof Map)) {
                    node = [(langTag): node]
                    // TODO: warn if a node isn't a dict:
                    //   "value was expected to be a lang dict but was %r."
                }
                if (node.size() == 0) {
                    continue
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

    protected boolean isLangMap(Object obj) {
        // TODO:IMPROVE: would otherwise fail (and mangle all nodes)!
        if (!langTag)
          return false
        if (obj instanceof Map) {
            for (key in ((Map)obj).keySet()) {
                if (key.startsWith(langTag)) {
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

}


// TODO: Turn QueryResult result into an interface with adapter impls.
class QueryResult implements Iterator, Iterable {
    TupleQueryResult tqResult
    QueryResult(TupleQueryResult tqResult) {
        this.tqResult = tqResult
    }
    List getBindingNames() {
        return tqResult.getBindingNames()
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
