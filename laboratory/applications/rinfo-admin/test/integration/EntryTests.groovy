import grails.test.*

class EntryTests extends GrailsUnitTestCase {
    def entryService
    def sessionFactory

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    /** 
    * Dummy org to use in feed test scenarios.
    **/
    Organization aTestOrg() {
        return new Organization(name: "Testorg", 
                homepage: "http://www.example.com", 
                contact_name: "Karl Karlsson",
                contact_email: "karl@example.com").save(flush:true)
    }

    void resetController(controller) {
        controller.request.removeAllParameters() 
        controller.response.setCommitted(false) 
        controller.response.reset() 
        controller.flash.message = "" 
        controller.params.clear()
    }

    /**
    * Make sure an entry is created when a Feed is created.
    */
    void testEntryAddedOnCreateFeed() {
        def fc = new FeedController()
        fc.entryService = entryService
        fc.params.url = "http://www.example.com/feed1"
        fc.params.identifier = "tag:example.com,2009:rinfo"
        fc.params.organization = aTestOrg()
        fc.save()

        def entries = Entry.findAllByItemClass("Feed", [sort: "lastUpdated", order:"asc", flush: true])
        assertEquals 1, entries.size()
        assertEquals "tag:lagrummet.se,2009:rinfo", entries[0].uri
    }


    /**
    * Make sure an entry is created when a Feed is updated.
    */
    void testEntryAddedOnUpdateFeed() {

        def anOrg = aTestOrg()
        def fc = new FeedController()
        fc.entryService = entryService
        fc.params.url = "http://www.example.com/feed1"
        fc.params.identifier = "tag:example.com,2009:rinfo"
        fc.params.organization = anOrg
        fc.save()
       
        sleep(1000)

        //Get the created feed instance
        def feedInstance = Feed.getAll()[0]

        resetController(fc)

        fc.params.id = feedInstance.id
        fc.params.identifier = feedInstance.identifier
        fc.params.organization = anOrg
        fc.params.url = "http://www.example.com/feed2"
        fc.update()

        def entries = Entry.findAllByItemClass("Feed", [sort: "lastUpdated", order:"asc"])

        assertEquals 2, entries.size()
        assertEquals "tag:lagrummet.se,2009:rinfo", entries[0].uri
        assertEquals "tag:lagrummet.se,2009:rinfo", entries[1].uri
    }

    /**
    * Make sure an entry is created when a Feed is deleted.
    */
    void testEntryAddedOnDeleteFeed() {

        def anOrg = aTestOrg()
        def fc = new FeedController()
        fc.entryService = entryService
        fc.params.url = "http://www.example.com/feed1"
        fc.params.identifier = "tag:example.com,2009:rinfo"
        fc.params.organization = anOrg
        fc.save()

        sleep(1000)
       
        //Get the created feed instance
        def feedInstance = Feed.getAll()[0]

        resetController(fc)

        fc.params.id = feedInstance.id
        fc.delete()

        def entries = Entry.findAllByItemClass("Feed", [sort: "lastUpdated", order:"asc"])

        assertEquals 2, entries.size()
        assertEquals "tag:lagrummet.se,2009:rinfo", entries[0].uri
        assertEquals "tag:lagrummet.se,2009:rinfo", entries[1].uri
    }


    /**
    * Make sure an entry is created when a Publicationcollection is created.
    */
    void testEntryAddedOnCreatePublicationcollection() {

        def c = new PublicationcollectionController()
        c.entryService = entryService
        c.params.name = "Testorg författningssamling"
        c.params.shortname = "TFS"
        c.params.homepage = "http://www.example.com/Lag--ratt/forfattningssamling1/"
        c.params.organization = aTestOrg()
        c.save()

        def pc = Publicationcollection.getAll()[0]
        def entries = Entry.findAllByItemClassAndItemId("Publicationcollection", pc.id, [sort: "lastUpdated", order:"asc", flush:true])

        assertEquals 1, entries.size()
        assertEquals pc.rinfoURI(), entries[0].uri
    }



    /**
    * Make sure an entry is created when a Publicationcollection is updated.
    */
    void testEntryAddedOnUpdatePublicationcollection() {

        def c = new PublicationcollectionController()
        c.entryService = entryService
        c.params.name = "Testorg författningssamling"
        c.params.shortname = "TFS"
        c.params.homepage = "http://www.example.com/Lag--ratt/forfattningssamling1/"
        c.params.organization = aTestOrg()
        c.save()

        def pc = Publicationcollection.getAll()[0]

        // Store the URI generated for this publicationcollection.
        def original_uri = pc.rinfoURI()

        resetController(c)

        c.params.id = pc.id
        c.params.name = "Testorg författningssamling 2"
        c.params.shortname = "TFS2"
        c.params.homepage = "http://www.example.com/Lag--ratt/forfattningssamling1/2"
        c.params.organization = pc.organization
        c.update()

        // Get all entries for this item
        def entries = Entry.findAllByItemClassAndItemId("Publicationcollection", pc.id, [sort: "lastUpdated", order:"asc", flush:true])

        assertEquals 2, entries.size()

        assertEquals original_uri, entries[0].uri
        assertEquals original_uri, entries[1].uri

        assertEquals entries[0].dateCreated, entries[1].dateCreated
    }



    /**
    * Make sure an entry is created when a Publicationcollection is deleted.
    */
    void testEntryAddedOnDeletePublicationcollection() {

        def c = new PublicationcollectionController()
        c.entryService = entryService
        c.params.name = "Testorg författningssamling"
        c.params.shortname = "TFS"
        c.params.homepage = "http://www.example.com/Lag--ratt/forfattningssamling1/"
        c.params.organization = aTestOrg()
        c.save()

        sleep(1000)

        def pc = Publicationcollection.getAll()[0]

        resetController(c)

        c.params.id = pc.id
        c.delete()

        // Make sure a new entry was created for the delete event
        def entries = Entry.findAllByItemClassAndItemId("Publicationcollection", pc.id, [sort: "lastUpdated", order:"desc", flush:true])
        println(entries)

        assertEquals 2, entries.size()
        assertEquals pc.rinfoURI(), entries[0].uri
        assert entries[0].dateDeleted != null
    }



    /**
    * Make sure an entry is created when an Organization is created.
    */
    void testEntryAddedOnCreateOrganization() {

        def c = new OrganizationController()
        c.entryService = entryService
        c.params.name = "Testorg"
        c.params.homepage = "http://www.example.com"
        c.params.contact_name = "Karl Karlsson"
        c.params.contact_email = "karl@example.com"
        c.save()

        def o = Organization.getAll()[0]
        def entries = Entry.findAllByItemClassAndItemId("Organization", o.id, [sort: "lastUpdated", order:"asc", flush:true])
        assertEquals 1, entries.size()
        assertEquals o.rinfoURI(), entries[0].uri
    }



    /**
    * Make sure an entry is created when an Organization is updated.
    */
    void testEntryAddedOnUpdateOrganization() {

        def c = new OrganizationController()
        c.entryService = entryService
        c.params.name = "Testorg"
        c.params.homepage = "http://www.example.com"
        c.params.contact_name = "Karl Karlsson"
        c.params.contact_email = "karl@example.com"
        c.save()

        sleep(1000)

        def o = Organization.getAll()[0]
        def original_uri = o.rinfoURI()
        resetController(c)

        c.params.id = o.id
        c.params.name = "Testorg 2"
        c.params.homepage = "http://www.example.com"
        c.params.contact_name = "Karl Karlsson"
        c.params.contact_email = "karl@example.com"
        c.update()

        // Get all entries for this item
        def entries = Entry.findAllByItemClassAndItemId("Organization", o.id, [sort: "lastUpdated", order:"asc", flush:true])
        assertEquals 2, entries.size()
        assertEquals original_uri, entries[0].uri
        assertEquals original_uri, entries[1].uri
        assertEquals entries[0].dateCreated, entries[1].dateCreated
   }



   // void testEntryAddedOnDeleteOrganization() {

   //     def org = new Organization(name: "Testorg", 
   //                                 homepage: "http://www.example.com", 
   //                                 contact_name: "Karl Karlsson",
   //                                 contact_email: "karl@example.com").save(flush:true)

   //     def original_uri = org.rinfoURI()

   //     //Verifiera att ett entry med samma URI som organisationen skapades
   //     def entries = Entry.findAllByItemClassAndItemId("Organization", org.id, [sort: "dateCreated", order:"asc", flush:true])

   //     assertEquals 1, entries.size()
   //     assertEquals org.rinfoURI(), entries[0].uri

   //     //Radera och verifiera att entry med deleteinformation skapas
   //     org.delete(flush:true)

   //     def entries2 = Entry.findAllByItemClassAndItemId("Organization", org.id, [sort: "id", order:"desc", flush:true])
   //     
   //     assertEquals 2, entries2.size()
   //     assert entries2[0].dateDeleted != null
   //     assertEquals original_uri, entries2[0].uri
   // }
}
