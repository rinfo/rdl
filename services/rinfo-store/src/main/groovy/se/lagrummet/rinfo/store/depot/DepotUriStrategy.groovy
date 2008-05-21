package se.lagrummet.rinfo.store.depot


// TODO: Make this configurable (extensible)!

class DepotUriStrategy {

    static final URI_PATTERN = ~"(/([^/]+)\\S+?)(?:/([^/,]+)(?:,([a-z]{2}))?)?"

    static final FEED_DIR_NAME = "feed"

    // TODO: Use URLConnection.getFileNameMap?
    static NAMED_MEDIA_TYPES = [
        atom: "application/atom+xml",
        feed: "application/atom+xml;type=feed",
        entry: "application/atom+xml;type=entry",
        rdf: "application/rdf+xml",
        html: "text/html",
        xhtml: "application/xhtml+xml",
        pdf: "application/pdf",
    ]
    static MEDIA_TYPE_HINTS = [:]
    static {
        NAMED_MEDIA_TYPES.each { k, v ->
            MEDIA_TYPE_HINTS[v] = k
        }
    }

    DepotUriStrategy() {
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

        if (NAMED_MEDIA_TYPES.containsKey(lastSansLang)) {
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
        return NAMED_MEDIA_TYPES.get(mediaHint)
    }

    String hintForMediaType(String mediaType) {
        return MEDIA_TYPE_HINTS.get(mediaType)
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
