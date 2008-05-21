package se.lagrummet.rinfo.store.depot


class DepotContent {
    File file
    String depotUriPath
    String mediaType
    String lang
    DepotContent(file, depotUriPath, mediaType, lang=null) {
        this.file = file
        this.depotUriPath = depotUriPath
        this.mediaType = mediaType
        this.lang = lang
    }
    String toString() {
        return "DepotContent(file:${file}" +
                ", depotUriPath:${depotUriPath}" +
                ", mediaType:${mediaType}" +
                ", lang:${lang})"
    }
}
