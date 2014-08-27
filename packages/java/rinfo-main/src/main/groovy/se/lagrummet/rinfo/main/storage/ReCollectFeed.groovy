package se.lagrummet.rinfo.main.storage
import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

class ReCollectFeed {
    static String generate(reCollectQueue) {
        def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        def stringWriter = new StringWriter()
        def xml = new MarkupBuilder(stringWriter)

        xml.feed(xmlns:'http://www.w3.org/2005/Atom') {
            title(type: "text", "rinfo recollect")
            id("tag:lagrummet.se,2014:rinfo:recollect")
            updated(sdf.format(new Date()))

            reCollectQueue.each {
                item -> entry {
                    id item.contentEntry.getId().toString()
                    title(type: "text", item.contentEntry.getTitle())
                    updated sdf.format(new Date())
                    content(contentType: item.contentEntry.getContentMimeType().toString(), src: item.contentEntry.getContentSrc()?.toString())
                }
            }
        }
        return xml.toString()
    }
}
