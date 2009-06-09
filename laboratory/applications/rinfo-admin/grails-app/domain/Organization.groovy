class Organization {
    
    static auditable = true //[handlersOnly:true]
    
    static constraints = {
        name(blank:false, maxSize:200)
        shortname(blank:true, nullable: true, maxSize:200)
        homepage(url:true, blank: false, maxSize:200)
        contact_name(blank:false, maxSize:200)
        contact_email(email:true, blank:false, maxSize:200)
        publicationcollections(blank:true)
        lastUpdated(nullable: true)
        dateCreated()
    }

    String name
    String shortname
    String homepage
    String contact_name
    String contact_email

    static hasMany = [ feeds : Feed, publicationcollections : Publicationcollection ]

    String toString() {
        if(shortname) {
            return name + ", " + shortname
        } else {
            return name
        }
    }


    // Skapa RDF-post för denna organisation
    String toRDF() {

        Writer sw = new StringWriter()
        def mb = new groovy.xml.MarkupBuilder(sw)
        mb.'rdf:RDF'(xmlns:"http://www.w3.org/1999/02/22-rdf-syntax-ns#", 'xmlns:foaf':"http://xmlns.com/foaf/0.1/") {
            'foaf:Organization'('rdf:about': this.rinfoURI()) {
                'foaf:name'('xml:lang': "sv", this.name)
            }
        }

        return sw.toString()
    }


    String rinfoURI() {
        return "http://rinfo.lagrummet.se/org/" + name.toLowerCase().replaceAll(" ", "_")
    }   

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
    

    def onDelete = {
        //Läs in första entry
        def entries = Entry.findAllByItemClassAndItemId("Organization", this.id, [sort: "dateCreated", order:"asc"])
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
        entry.title = this.name + " raderades"
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
        entry.title = this.name + " skapades"
        entry.uri = this.rinfoURI()
        entry.content = this.toRDF()
        entry.content_md5 = ""
        entry.dateCreated = entry_date
        entry.save()

    }


    // Skapa ett nytt atom entry när posten uppdateras
    def onChange = { oldMap,newMap ->

        //Läs in tidigare entry
        def entries = Entry.findAllByItemClassAndItemId("Organization", this.id, [sort: "dateCreated", order:"asc"])
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
        entry.title = this.name + " ändrades"
        entry.uri = first_entry.uri
        entry.content = this.toRDF()
        entry.content_md5 = ""
        entry.save()
    }
}
