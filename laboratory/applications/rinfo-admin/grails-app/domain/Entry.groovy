class Entry {

    static constraints = {
        title(blank:false)
        content(blank:true)
        content_md5(blank:true)
        uri(blank: false)
        dateDeleted(blank:true, nullable:true)
        dateCreated(blank:false, nullable:true)
        lastUpdated(blank:false)
        itemClass(blank:false)
        itemId(blank:false)
    }
   
    static mapping = {
        autoTimestamp false
    }


    String title
    String content
    String content_md5
    String uri

    Integer itemId
    String itemClass

    def relateTo(item) {
        this.itemClass = item.class.name
        this.itemId = item.id
    }

    Date dateDeleted
    Date dateCreated
    Date lastUpdated


    String toString() {
        StringWriter sw = new StringWriter()
        sw.append("\n\ttitle: " + title)
        sw.append("\n\tcontent: " + content)
        sw.append("\n\tcontent_md5: " + content_md5)
        sw.append("\n\turi: " + uri)
        sw.append("\n\titemId: " + itemId)
        sw.append("\n\titemClass: " + itemClass)
        sw.append("\n\tdateDeleted: " + dateDeleted)
        sw.append("\n\tdateCreated: " + dateCreated)
        sw.append("\n\tlastUpdated: " + lastUpdated + "\n")
        return sw.toString()
    }
}
