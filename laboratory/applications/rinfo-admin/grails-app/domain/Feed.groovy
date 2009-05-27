class Feed {

    static constraints = {
        lastUpdated(nullable: true)
    }

    static belongsTo = Organization
    Organization organization    

    String url
    String identifier

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
