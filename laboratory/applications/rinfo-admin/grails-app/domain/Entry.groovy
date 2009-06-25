/**
* Represents an <a href="http://tools.ietf.org/html/rfc4287#section-4.1.2">Atom
* entry</a> and is used to provide data about Organizations,
* Publication collections and Feeds to the main application.
*/
class Entry {

    static constraints = {
        title(blank:false)
        content(blank:true)
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
    String uri

    /**
    * This maps the current entry instance to an instance of some other type
    * when used together with itemClass.
    */
    Integer itemId

    /**
    * This maps the current entry instance to an instance of some other type
    * when used together with itemId.
    */
    String itemClass


    Date dateDeleted
    Date dateCreated
    Date lastUpdated

    /**
    * Loosely relates this entry to an instance of some other type instead of using belongsTo.
    */
    def relateTo(item) {
        this.itemClass = item.class.name
        this.itemId = item.id
    }

    String toString() {
        StringWriter sw = new StringWriter()
        sw.append("\n\ttitle: " + title)
        sw.append("\n\tcontent: " + content)
        sw.append("\n\turi: " + uri)
        sw.append("\n\titemId: " + itemId)
        sw.append("\n\titemClass: " + itemClass)
        sw.append("\n\tdateDeleted: " + dateDeleted)
        sw.append("\n\tdateCreated: " + dateCreated)
        sw.append("\n\tlastUpdated: " + lastUpdated + "\n")
        return sw.toString()
    }
}
