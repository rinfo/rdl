package se.lagrummet.rinfo.store.depot

class ParsedPath {

    String collection
    String depotUriPath
    String mediaHint
    String lang
    boolean asDir

    ParsedPath(collection, depotUriPath, asDir=false, mediaHint=null, lang=null) {
        this.collection = collection
        this.depotUriPath = depotUriPath
        this.mediaHint = mediaHint
        this.lang = lang
        this.asDir = asDir
    }

    boolean equals(Object other) {
        if (!other instanceof ParsedPath) {
            return other.equals(this)
        }
        return (collection == other.collection &&
                depotUriPath == other.depotUriPath &&
                asDir == other.asDir &&
                mediaHint == other.mediaHint &&
                lang == other.lang)
    }

    String toString() {
        return "ParsedPath(${collection}" +
                ", ${depotUriPath}" +
                ", ${asDir}" +
                ", ${mediaHint}" +
                ", ${lang})"
    }

}
