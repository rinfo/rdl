import org.jsecurity.crypto.hash.Sha1Hash

class BootStrap {

    def init = { servletContext ->
        // Skapa en exempeladministratör
        def adminRole = new JsecRole(name: "Administrator").save()
        def adminUser = new JsecUser(username: "admin", passwordHash: new Sha1Hash("admin").toHex()).save()
        new JsecUserRoleRel(user: adminUser, role: adminRole).save()
    }
    def destroy = {
    }


} 
