class Organization {

    static constraints = {
        lastUpdated(nullable: true)
    }

    String name
    String homepage
    String contact_name
    String contact_email

    static hasMany = [ feeds : Feed, publicationcollections : Publicationcollection ]

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
