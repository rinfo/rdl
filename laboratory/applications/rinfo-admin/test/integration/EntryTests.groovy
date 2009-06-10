import grails.test.*

class EntryTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }


    /**
    * Make sure an entry is created when a Publicationcollection is created.
    */
    void testEntryAddedOnCreatePublicationcollection() {

        // First, create an organization to use as a parent for the
        // Publicationcollection.
        def org = new Organization(name: "Testorg", 
                                    homepage: "http://www.example.com", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save(flush:true)

        // Create and connect a Publicationcollection.
        def pc = new Publicationcollection(name: "Testorg författningssamling",
                shortname: "TFS",
                homepage:"http://www.example.com/Lag--ratt/forfattningssamling1/",
                organization: org).save(flush:true)

        def entries = Entry.findAllByItemClassAndItemId("Publicationcollection", pc.id, [sort: "dateCreated", order:"asc", flush:true])

        assertEquals 1, entries.size()
        assertEquals pc.rinfoURI(), entries[0].uri
    }





    void testEntryAddedOnUpdatePublicationcollection() {

        def org = new Organization(name: "Testorg3", 
                                    homepage: "http://www.example.com", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save(flush:true)

        // Create and connect a Publicationcollection.
        def pc = new Publicationcollection(name: "Testorg författningssamling",
                shortname: "TFS",
                homepage:"http://www.example.com/Lag--ratt/forfattningssamling1/",
                organization: org).save(flush:true)

        // Store the URI generated for this publicationcollection.
        def original_uri = pc.rinfoURI()

        // Update the publicationcollection.
        def pc2 = Publicationcollection.get(pc.id)
        pc2.name = "Testorg2 författningssamling"
        pc2.save(flush:true)

        def entries = Entry.findAllByItemClassAndItemId("Publicationcollection", pc.id, [sort: "dateCreated", order:"asc", flush:true])

        assertEquals 2, entries.size()

        assertEquals original_uri, entries[0].uri
        assertEquals original_uri, entries[1].uri

        assertEquals entries[0].dateCreated, entries[1].dateCreated
    }



    void testEntryAddedOnCreateOrganization() {

        def org = new Organization(name: "Testorg", 
                                    homepage: "http://www.example.com", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save(flush:true)

        def entries = Entry.findAllByItemClassAndItemId("Organization", org.id, [sort: "dateCreated", order:"asc", flush:true])

        assertEquals 1, entries.size()
        assertEquals org.rinfoURI(), entries[0].uri
    }



    void testEntryAddedOnUpdateOrganization() {

        def org = new Organization(name: "Testorg3", 
                                    homepage: "http://www.example.com", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save(flush:true)

        // Store the URI generated for this organization.
        def original_uri = org.rinfoURI()

        // Update the organization.
        def org2 = Organization.get(org.id)
        org2.name = "Testorg4"
        org2.save(flush:true)

        def orgentries = Entry.list(flush:true)

        assertEquals 2, orgentries.size()

        assertEquals original_uri, orgentries[0].uri
        assertEquals original_uri, orgentries[1].uri

        assertEquals orgentries[0].dateCreated, orgentries[1].dateCreated
    }



    void testEntryAddedOnDeleteOrganization() {

        def org = new Organization(name: "Testorg", 
                                    homepage: "http://www.example.com", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save(flush:true)

        def original_uri = org.rinfoURI()

        //Verifiera att ett entry med samma URI som organisationen skapades
        def entries = Entry.findAllByItemClassAndItemId("Organization", org.id, [sort: "dateCreated", order:"asc", flush:true])

        assertEquals 1, entries.size()
        assertEquals org.rinfoURI(), entries[0].uri

        //Radera och verifiera att entry med deleteinformation skapas
        org.delete(flush:true)

        entries = Entry.findAllByItemClassAndItemId("Organization", org.id, [sort: "lastUpdated", order:"desc", flush:true])
        
        println(entries[0])
        println(entries[1])
        assert entries[0].dateDeleted != null
        assertEquals original_uri, entries[0].uri

    }
}
