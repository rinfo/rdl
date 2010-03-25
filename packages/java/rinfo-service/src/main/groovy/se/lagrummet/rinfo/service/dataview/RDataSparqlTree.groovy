package se.lagrummet.rinfo.service.dataview


import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree


class RDataSparqlTree extends SparqlTree {

    String locale
    String localeKey
    String resourceBaseUrl

    RDataSparqlTree(String resourceBaseUrl, String locale) {
        setUriKey("resource_uri")
        this.locale = locale
        this.localeKey = langTag+locale
        this.resourceBaseUrl = resourceBaseUrl
    }

    Object completeNode(Object node, String key, Map parentNode) {
        // TODO: html-escape text (unless xmlliterals)
        if (isDatatypeNode(node)) {
            return node[valueKey]
        } else if (isLangNode(node)) {
            return node[localeKey]
        } else if (isResource(node)) {
            def uri = node[uriKey]
            if (uri) {
                node['uri_term'] = uriTerm(uri)
                if (uri.startsWith(resourceBaseUrl)) {
                    def path = uri.substring(resourceBaseUrl.size())
                    //switch (path) {
                    //    case ~/publ\/.+/:
                    //    break
                    //}
                    node['rdata_url'] = "/rdata/" + path
                }
            }
        }
        return node
    }

    String uriTerm(uri) {
        return uri.substring(
                uri.lastIndexOf(uri.contains('#')? '#' : '/') + 1,
                uri.size())
    }

}
