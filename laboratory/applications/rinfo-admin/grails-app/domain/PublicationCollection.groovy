class PublicationCollection {

    static constraints = {
        lastUpdated(nullable: true)
    }

    static belongsTo = Organization
    Organization organization    

    String name
    String shortname
    String homepage

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
