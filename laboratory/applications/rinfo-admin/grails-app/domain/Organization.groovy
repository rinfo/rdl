class Organization {

    static constraints = {
        name(blank:false, maxSize:200)
        homepage(url:true, blank: false, maxSize:200)
        contact_name(blank:false, maxSize:200)
        contact_email(email:true, blank:false, maxSize:200)
        publicationcollections(blank:true)
        lastUpdated(nullable: true)
        dateCreated()
    }

    String name
    String homepage
    String contact_name
    String contact_email

    static hasMany = [ feeds : Feed, publicationcollections : Publicationcollection ]

    String toString() {
        return name
    }

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
