package se.lagrummet.rinfo.store.depot

import org.apache.commons.io.FileUtils
import org.apache.commons.codec.digest.DigestUtils


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

    String getMd5Hex() {
        return DigestUtils.md5Hex(FileUtils.readFileToByteArray(file))
    }

    String toString() {
        return "DepotContent(file:${file}" +
                ", depotUriPath:${depotUriPath}" +
                ", mediaType:${mediaType}" +
                ", lang:${lang})"
    }

}
