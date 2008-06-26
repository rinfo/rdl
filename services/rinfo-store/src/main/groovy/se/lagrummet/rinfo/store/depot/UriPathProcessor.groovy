package se.lagrummet.rinfo.store.depot


// TODO: Make this configurable/extensible

class UriPathProcessor {

    static final URI_PATTERN = ~"(/([^/]+)\\S+?)(?:/([^/,]+)(?:,([a-z]{2}))?)?"

    // TODO: Use URLConnection.getFileNameMap?
    static DEFAULT_NAMED_MEDIA_TYPES = [
        atom: "application/atom+xml",
        feed: "application/atom+xml;type=feed",
        entry: "application/atom+xml;type=entry",
        rdf: "application/rdf+xml",
        html: "text/html",
        xhtml: "application/xhtml+xml",
        pdf: "application/pdf",
    ]

    Map namedMediaTypes
    private Map mediaTypeHints

    UriPathProcessor() {
        setNamedMediaTypes(DEFAULT_NAMED_MEDIA_TYPES)
    }

    void setNamedMediaTypes(Map namedMediaTypes) {
        this.namedMediaTypes = Collections.unmodifiableMap(namedMediaTypes)
        mediaTypeHints = [:]
        namedMediaTypes.each { k, v ->
            mediaTypeHints[v] = k
        }
    }

    ParsedPath parseUriPath(String uriPath) {
        def matcher = URI_PATTERN.matcher(uriPath)
        if (!matcher.matches()) {
            return null
        }
        def collection
        def depotUriPath
        def mediaHint = null
        def lang = null
        def asDir = false

        def g = matcher.&group
        depotUriPath = g(1)
        collection = g(2)
        def lastSansLang = g(3)
        lang = g(4)

        if (namedMediaTypes.containsKey(lastSansLang)) {
            mediaHint = lastSansLang
        } else {
            // re-combine
            if (lastSansLang)
                depotUriPath += "/" + lastSansLang
        }
        if (depotUriPath.endsWith("/")) {
            asDir = true
            depotUriPath = depotUriPath.substring(0, depotUriPath.length()-1)
        }
        return new ParsedPath(collection, depotUriPath, asDir, mediaHint, lang)
    }

    String mediaTypeForHint(String mediaHint) {
        return namedMediaTypes.get(mediaHint)
    }

    String hintForMediaType(String mediaType) {
        return mediaTypeHints.get(mediaType)
    }

    String makeNegotiatedUriPath(String entryUriPath, String mediaType,
            String lang=null) {
        def mediaHint = hintForMediaType(mediaType)
        assert mediaHint, mediaType
        def uri = "${entryUriPath}/${mediaHint}"
        if (lang) {
            uri += ",${lang}"
        }
        return uri
    }

}
