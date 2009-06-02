import org.jsecurity.crypto.hash.Sha1Hash

class BootStrap {

    def init = { servletContext ->
        // Skapa en exempeladministratör
        def adminRole = new JsecRole(name: "Administrator").save()
        def adminUser = new JsecUser(username: "admin", passwordHash: new Sha1Hash("admin").toHex()).save()
        new JsecUserRoleRel(user: adminUser, role: adminRole).save()


        //Lite exempeldata
        def org1 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        
        def org2 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org3 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org4 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org5 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org6 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org7 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org16 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org8 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org9 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org10 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org11 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org12 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org13 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def org14 = new Organization(name: "Boverket", 
                                    homepage: "http://www.boverket.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()
        def feed1 = new Feed(organization: org1, url: "http://example.com/feed1", identifier: "2009,bofeed").save()

        def org15 = new Organization(name: "Domstolsverket", 
                                    homepage: "http://www.domstol.se", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save()

        def feed2 = new Feed(organization: org2, url: "http://example.com/feed2", identifier: "2009,dvfeed").save()
    }



    def destroy = {
    }


} 
