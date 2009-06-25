/**
* Represents an organization, typically a government agency. The main task is
* to provide the legal information system with basic information about
* participating organizations.
*/
class Organization {
    
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

    /**
    * The formal name (e.g. "Verket för förvaltningsutveckling").
    */
    String name

    /**
    * The formal short name if available (e.g. "VERVA").
    */
    String shortname

    String homepage
    String contact_name
    String contact_email

    Date dateCreated
    Date lastUpdated

    static hasMany = [ feeds : Feed, publicationcollections : Publicationcollection ]

    String toString() {
        if(shortname) {
            return name + ", " + shortname
        } else {
            return name
        }
    }

    /**
    * The RDF representation of this instance. This information is used by
    * agencies to relate laws and other legal information to specific
    * organizations.
    */
    String toEntryContent() {
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
        return "http://rinfo.lagrummet.se/org/" + name.toLowerCase().replaceAll(" ", "_").replaceAll("ö","o").replaceAll("ä","a").replaceAll("å","a")
    }   
    
}
