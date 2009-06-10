import org.jsecurity.crypto.hash.Sha1Hash
import grails.util.GrailsUtil 

class BootStrap {

    def init = { servletContext ->

        switch(GrailsUtil.environment) {  
            case "development":

                def adminRole = new JsecRole(name: "Administrator").save()
                def adminUser = new JsecUser(username: "admin", passwordHash: new Sha1Hash("admin").toHex()).save()
                new JsecUserRoleRel(user: adminUser, role: adminRole).save()



                //Lite exempeldata
                def org1 = new Organization(name: "Boverket", 
                        homepage: "http://www.boverket.se", 
                        contact_name: "Bengt Bengtsson",
                        contact_email: "karl@example.com").save()

                
                def ffs1 = new Publicationcollection(name: "Boverkets författningssamling",
                        shortname: "BFS",
                        homepage:"http://www.boverket.se/Lag--ratt/Boverkets-forfattningssamling1/",
                        organization: org1).save()

                def feed1 = new Feed(organization: org1, url: "http://www.example.com/feed1", identifier: "tag:boverket.se,2009:data").save()

                def org2 = new Organization(name: "Arbetsförmedlingen", 
                        homepage: "http://www.ams.se", 
                        contact_name: "Anna Andersson",
                        contact_email: "anna@example.com").save()

            break


            case "test":
                log.info( 'BootStrap test' )

                // Skapa en exempeladministratör
                def adminRole = new JsecRole(name: "Administrator").save()
                def adminUser = new JsecUser(username: "admin", passwordHash: new Sha1Hash("admin").toHex()).save()
                new JsecUserRoleRel(user: adminUser, role: adminRole).save()
            break

            case "production":
                log.info( 'BootStrap production' )
            break
        }
    }

    def destroy = {
    }

} 
