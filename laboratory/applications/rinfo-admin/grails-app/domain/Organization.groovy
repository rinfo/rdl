class Organization {

    static constraints = {
        name(blank:false, maxSize:200)
        homepage(blank:false, maxSize:200)
        contact_name(blank:false, maxSize:200)
        contact_email(blank:false, maxSize:200)
        publicationcollections(blank:true)
        lastUpdated(nullable: true)
        dateCreated()
    }

    String name
    URL homepage
    String contact_name
    String contact_email

    static hasMany = [ feeds : Feed, publicationcollections : Publicationcollection ]

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
