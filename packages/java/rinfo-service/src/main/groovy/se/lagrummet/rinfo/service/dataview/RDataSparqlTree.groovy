package se.lagrummet.rinfo.service.dataview


import se.lagrummet.rinfo.base.rdf.sparqltree.SparqlTree


class RDataSparqlTree extends SparqlTree {

    String locale
    String localeKey
    Map appData
    Map basePrefixMap

    RDataSparqlTree(Map appData, String locale) {
        setUriKey("resource_uri")
        this.locale = locale
        this.localeKey = langTag+locale
        this.appData = appData
        basePrefixMap = [:]
        appData.profile.prefix.each { k, v ->
            basePrefixMap[v] = k
        }
    }

    Object completeNode(Object node, String key, Map parentNode) {
        // TODO: html-escape text (unless xmlliterals)
        if (isDatatypeNode(node)) {
            def value = node[valueKey]
            if (node[datatypeKey] == appData.profile.prefix['xsd']+'date') {
                node['year'] = value[0..value.indexOf("-")-1]
                node['monthDay'] = value[value.indexOf("-")+1..-1]
                node['value'] = value
                return node
            }
            return value
        } else if (isLangNode(node)) {
            return node[localeKey]
        } else if (isResource(node)) {
            def uri = node[uriKey]
            if (uri) {
                def baseUrl = appData.resourceBaseUrl
                    def term = getUriTerm(uri)
                node['uri_term'] = term
                node['qname'] = qname(uri, term)
                if (uri.startsWith(baseUrl)) {
                    def path = uri.substring(baseUrl.size())
                    //switch (path) {
                    //    case ~/publ\/.+/:
                    //    break
                    //}
                    node['view_url'] = appData.basePath + "/" + path
                }
            }
        }
        return node
    }

    String getUriTerm(uri) {
        return uri.substring(
                uri.lastIndexOf(uri.contains('#')? '#' : '/') + 1,
                uri.size())
    }

    String qname(uri, term) {
        def basePart = uri.substring(0, uri.size()-term.size())
        def pfx = basePrefixMap[basePart]
        if (pfx == null)
            return
        return pfx+":"+term
    }

}
