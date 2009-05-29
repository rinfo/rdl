class Feed {

    static constraints = {
        url(blank:false, maxSize:400)
        identifier(blank:false, maxSize:300)
        organization(blank:false) 
        lastUpdated(nullable: true)
        dateCreated()
    }

    static belongsTo = Organization
    Organization organization    

    URL url
    String identifier

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
