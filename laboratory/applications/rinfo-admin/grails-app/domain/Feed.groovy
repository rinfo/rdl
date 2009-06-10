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

    String url
    String identifier

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

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated



    def onDelete = {
        //Läs in första entry
        def entries = Entry.findAllByItemClassAndItemId(this.class.name, this.id, [sort: "dateCreated", order:"asc"])
        //Välj den första posten
        def first_entry
        if(entries) {
            first_entry = entries[0]
        }

        //Skapa det nya entryt
        def entry_date = new Date()
        def entry = new Entry()
        entry.relateTo(this)
        entry.lastUpdated = entry_date
        entry.dateDeleted = entry_date
        entry.dateCreated = first_entry.dateCreated
        entry.title = this.identifier + " raderades"
        entry.uri = first_entry.uri
        entry.content = this.toRDF()
        entry.content_md5 = ""
        entry.save()
    }


    // Skapa ett nytt atom entry när posten skapas
    def onSave = {

        def entry_date = new Date()
        def entry = new Entry()
        entry.relateTo(this)
        entry.lastUpdated = entry_date
        entry.title = this.identifier + " skapades"
        entry.uri = this.rinfoURI()
        entry.content = this.toRDF()
        entry.content_md5 = ""
        entry.dateCreated = entry_date
        entry.save()

    }



    // Skapa ett nytt atom entry när posten uppdateras
    def onChange = { oldMap,newMap ->
        //Läs in tidigare entry
        def entries = Entry.findAllByItemClassAndItemId(this.class.name, this.id, [sort: "dateCreated", order:"asc"])
        //Välj den första posten
        def first_entry
        if(entries) {
            first_entry = entries[0]
        }

        //Skapa det nya entryt
        def entry_date = new Date()
        def entry = new Entry()
        entry.relateTo(this)
        entry.lastUpdated = entry_date
        entry.dateCreated = first_entry.dateCreated
        entry.title = this.identifier + " ändrades"
        entry.uri = first_entry.uri
        entry.content = this.toRDF()
        entry.content_md5 = ""
        entry.save()
    }

}
