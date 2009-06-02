class Publicationcollection {

    static constraints = {
        name(blank:false, maxSize:500)
        shortname(blank:false, maxSize:500)
        organization()
        homepage(url:true, blank:false, maxSize:400)
        lastUpdated(nullable: true)
        dateCreated()
    }

    static belongsTo = Organization
    Organization organization    

    String name
    String shortname
    String homepage

    String toString() { 
        return name + " (" + shortname + ")
    }

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
