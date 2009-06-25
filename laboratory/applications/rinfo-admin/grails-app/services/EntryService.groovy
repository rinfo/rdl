class EntryService {

    /**
    * Create a new Atom entry to notify the main application that an
    * object has been created or updated. Since the dateCreated information
    * should represent the original item instance, the first entry is loaded
    * and the dateCreated and URI is used from that entry.
    */
    void createEntry(item) {
        def first_entry
        def date_created = new Date()
        def last_updated = date_created
        def entry = new Entry()
        def entries

        switch (item.class) {
            case Feed:
                entries = Entry.findAllByItemClass(item.class.name, [sort: "lastUpdated", order:"asc", flush:true])
                entry.title = "Insamlingskälla skapades"
                entry.uri = "tag:lagrummet.se,2009:rinfo"
                entry.content = Feed.toEntryContent()
                break
            default:
                entries = Entry.findAllByItemClassAndItemId(item.class.name, item.id, [sort: "lastUpdated", order:"asc", flush:true])
                if(entries) {
                    entry.title = item.class.name + " uppdaterades"
                    entry.uri = entries[0].uri
                } else {
                    entry.title = item.class.name + " skapades"
                    entry.uri = item.rinfoURI()
                }
                entry.content = item.toEntryContent()
        }

        if(entries) {
            first_entry = entries[0]
            date_created = first_entry.dateCreated
        }

        entry.relateTo(item)
        entry.dateCreated = date_created
        entry.lastUpdated = last_updated
        entry.save(flush:true)
    }
    

    /**
    * Create a new Atom entry to notify the main application that aFeed item
    * has been deleted. Since the dateCreated information should represent the
    * original item instance, the first entry is loaded and the dateCreated and
    * URI is used from that entry. Since Feed items are treated in bulk, this
    * will result in a regular Atom entry element.
    */
    void deleteItem(Feed item) {
        def entries = Entry.findAllByItemClass(item.class.name, [sort: "lastUpdated", order:"asc"])
        def first_entry
        def entry_date = new Date()
        def entry = new Entry()
        if(entries) {
            first_entry = entries[0]
            entry.dateCreated = first_entry.dateCreated
        } else {
            entry.dateCreated = entry_date
        }

        entry.relateTo(item)
        entry.lastUpdated = entry_date
        entry.title = item.identifier + " raderades"
        entry.uri = "tag:lagrummet.se,2009:rinfo"
        entry.content = Feed.toEntryContent()

        item.delete()

        entry.save()
    }


    /**
    * Create a new Atom entry to notify the main application that an
    * item has been deleted. Since the dateCreated information should
    * represent the original organization instance, the first entry is loaded
    * and the dateCreated and URI is used from that entry.
    */
    void deleteItem(item) {
        def entries = Entry.findAllByItemClassAndId(item.class.name, item.id, [sort: "lastUpdated", order:"asc"])
        def first_entry
        def entry = new Entry()
        def entry_date = new Date()
        if(entries) {
            first_entry = entries[0]
            entry.dateCreated = first_entry.dateCreated
            entry.uri = first_entry.uri
        } else {
            entry.dateCreated = entry_date
            entry.uri = item.rinfoURI()
        }

        entry.relateTo(item)
        entry.lastUpdated = entry_date
        entry.dateDeleted = entry_date
        entry.title = item.toString() + " raderades"
        entry.content = item.toEntryContent()

        item.delete()

        entry.save()
    }
}
