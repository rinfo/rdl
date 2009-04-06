package se.lagrummet.rinfo.base.rdf.sparqltree

class SmartLens implements Lens {

    static EXPOSED_URI_KEY = 'resource_uri'
    static URI_TERM_KEY = 'uri_term'
    static VIA_KEY  = 'ref_via'

    String locale
    String langKey

    SmartLens(String locale) {
        this.locale = locale
        langKey = '@' + (locale ?: "")
    }

    Map newResource(Map node) {
        def copy = new HashMap(node)
        def uri = copy[SparqlTree.URI_KEY]
        if (uri != null) {
            copy[EXPOSED_URI_KEY] = uri
            copy[URI_TERM_KEY] = uriTerm(uri)
        }
        return copy
    }

    void mergeResources(Map source, Map result) {
        result.putAll(source)
    }

    void updateVia(resource, viaPair) {
        def (viaKey, viaResource) = viaPair
        if (!resource.containsKey(VIA_KEY)) {
            resource[VIA_KEY] = newViaObject()
        }
        def via = resource[VIA_KEY]
        if (!via.containsKey(viaKey)) {
            via[viaKey] = []
        }
        via[viaKey].add(viaResource)
    }

    protected Map newViaObject() {
        return [:]
    }

    Object castLiteral(Object node) {
        if (SparqlTree.isLangNode(node)) {
            if (!node.isEmpty())
                return node[langKey] ?: node.values().iterator().next()
            else
                return null
        }
        return node
    }

    static String uriTerm(uri) {
        return uri.substring(uri.lastIndexOf(
                uri.contains('#')? '#' : '/'
            )+1, uri.size())
    }

}
