import grails.test.*

class EntryTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testEntryAddedOnCreateOrganization() {

        def org = new Organization(name: "Testorg", 
                                    homepage: "http://www.example.com", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save(flush:true)

        //Verifiera att ett entry med samma URI som organisationen skapades
        def entries = Entry.findAllByItemClassAndItemId("Organization", org.id, [sort: "dateCreated", order:"asc", flush:true])

        assertEquals 1, entries.size()
        assertEquals org.rinfoURI(), entries[0].uri
    }




    void testEntryAddedOnUpdateOrganization() {

        def org = new Organization(name: "Testorg3", 
                                    homepage: "http://www.example.com", 
                                    contact_name: "Karl Karlsson",
                                    contact_email: "karl@example.com").save(flush:true)

        def original_uri = org.rinfoURI()

        //Läs in och spara om organisationen för att trigga en uppdatering
        def org2 = Organization.get(org.id)
        org2.name = "Testorg4"
        org2.save(flush:true)

        def orgentries = Entry.list(flush:true)

        //Verifiera att det finns två entries (för create och update)
        assertEquals 2, orgentries.size()

        //Båda skall ha samma URI
        assertEquals original_uri, orgentries[0].uri
        assertEquals original_uri, orgentries[1].uri

        //Båda skall ha samma dateCreated
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
