package se.lagrummet.rinfo.store.depot


// TODO: Make this configurable (extensible)!

class DepotUriStrategy {

    static final URI_PATTERN = ~/(\/([a-z0-9_-]+)\/[a-z0-9_\-\/:,\.]+?)(?:(\/)|(?:\/([a-z0-9]+)(?:,([a-z]{2}))?))?/

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
        def g = matcher.&group
        def collection = g(2)
        def depotUriPath = g(1)
        def mediaHint = g(4)
        if (mediaHint != null && !NAMED_MEDIA_TYPES.containsKey(mediaHint)) {
            // re-add as path leaf
            depotUriPath += "/" + mediaHint
            mediaHint = null
        }
        def lang = g(5)
        def asDir = uriPath.endsWith("/")
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
