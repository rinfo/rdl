/**
* Represents an atom feed where legal information from an organization can be picked up.
*/
class Feed {

    static auditable = [handlersOnly:true]

    static constraints = {
        url(url:true, blank:false, maxSize:400)
        identifier(blank:false, maxSize:300)
        organization(blank:false) 
        lastUpdated(nullable: true)
        dateCreated()
    }

    static belongsTo = Organization
    Organization organization    

    /**
    * The current location of the feed. 
    * @see identifier
    */
    String url

    /**
    * The long term URI identifier for a feed. Is not necessarily the same as
    * the URL where it resides. This identifier will likely be a <a
    * href="http://www.faqs.org/rfcs/rfc4151.html">Tag URI</a> to enable
    * different locations for the feed.
    */
    String identifier

    Date dateCreated
    Date lastUpdated

    String toString() { 
        return "Källa: " + url
    }

    String toRDF() {

        Writer sw = new StringWriter()
        def mb = new groovy.xml.MarkupBuilder(sw)
        mb.'rdf:RDF'('xmlns:rdf':"http://www.w3.org/1999/02/22-rdf-syntax-ns#", 
                    'xmlns:foaf':"http://xmlns.com/foaf/0.1/",
                    'xmlns:dct':"http://purl.org/dc/terms/",
                    'xmlns:iana':"http://www.iana.org/assignments/relation/",
                    'xmlns:awol':"http://bblfish.net/work/atom-owl/2006-06-06/#") {
            'awol:Feed'('rdf:about': this.rinfoURI()) {
                'dct:publisher'('rdf:resource': this.organization.rinfoURI())
                'iana:current'('rdf:resource': this.url)
                'dct:publisher'('rdf:resource': this.organization.rinfoURI())
                'awol:id'('rdf:datatype': "http://www.w3.org/2001/XMLSchema#anyURI", this.rinfoURI())
            }
        }

        return sw.toString()
    }

    String rinfoURI() {
        return identifier
   }   




    def onDelete = {
        // Pick up first entry for this item        
        def entries = Entry.findAllByItemClassAndItemId(this.class.name, this.id, [sort: "dateCreated", order:"asc"])
        def first_entry
        if(entries) {
            first_entry = entries[0]
        }

        def entry_date = new Date()

        // Create the new entry
        def entry = new Entry()
        entry.relateTo(this)
        entry.lastUpdated = entry_date
        entry.dateDeleted = entry_date
        entry.dateCreated = first_entry.dateCreated
        entry.title = this.identifier + " raderades"
        entry.uri = first_entry.uri
        entry.content = this.toRDF()
        entry.save()
    }

    def onSave = {

        def entry_date = new Date()
        def entry = new Entry()
        entry.relateTo(this)
        entry.lastUpdated = entry_date
        entry.title = this.identifier + " skapades"
        entry.uri = this.rinfoURI()
        entry.content = this.toRDF()
        entry.dateCreated = entry_date
        entry.save()

    }



    def onChange = {
        def entries = Entry.findAllByItemClassAndItemId(this.class.name, this.id, [sort: "dateCreated", order:"asc"])
        def first_entry
        if(entries) {
            first_entry = entries[0]
        }

        def entry_date = new Date()
        def entry = new Entry()
        entry.relateTo(this)
        entry.lastUpdated = entry_date
        entry.dateCreated = first_entry.dateCreated
        entry.title = this.identifier + " ändrades"
        entry.uri = first_entry.uri
        entry.content = this.toRDF()
        entry.save()
    }

}
