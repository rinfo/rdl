package se.lagrummet.rinfo.store.depot

import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.AtomDate
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI

import org.apache.abdera.ext.history.FeedPagingHelper as Paging
import org.apache.abdera.ext.sharing.SharingHelper

import javax.xml.namespace.QName


class Atomizer {

    private final logger = LoggerFactory.getLogger(Atomizer)


    // TODO: must be in Abdera..
    static final ATOM_ENTRY_MEDIA_TYPE = "application/atom+xml;type=entry"

    static final ENTRY_EXT_GDATA_DELETED = new QName(
            "http://schemas.google.com/g/2005", "deleted", "gd")

    static final LINK_EXT_MD5 = new QName(
            "http://purl.org/atompub/link-extensions/1.0", "md5", "le")

    static final FEED_EXT_TOMBSTONE = new QName(
            "http://purl.org/atompub/tombstones/1.0", "deleted-entry", "at")


    FileDepot depot

    // TODO: Atomizer config properties
    def includeDeleted = true

    def useEntrySelfLink = true
    def useLinkExtensionsMd5 = true
    def useTombstones = true
    def useFeedSync = true
    def useGdataDeleted = true


    Atomizer(FileDepot depot) {
        this.depot = depot
    }

    static final DEFAULT_FEED_BATCH_SIZE = 25
    int feedBatchSize

    int getFeedBatchSize() {
        return feedBatchSize ?: DEFAULT_FEED_BATCH_SIZE
    }

    void generateIndex() {
        def feedDir = new File(depot.baseDir, depot.feedPath)
        if (!feedDir.exists()) {
            feedDir.mkdir()
        }
        def ascDateSortedEntryRefs = new TreeSet(
                [compare: { a, b ->
                    if (a.date == b.date) {
                        return a.uriPath.compareTo(b.uriPath)
                    }
                    return a.date.compareTo(b.date)
                }] as Comparator
            )
        // only keeping necessary data to minimize memory use
        // TODO: includeHistorical=true
        for (depotEntry in depot.iterateEntries(false, includeDeleted)) {
            ascDateSortedEntryRefs.add(
                    new EntryRef(uriPath: depotEntry.entryUriPath,
                            date: depotEntry.updated) )
        }
        indexEntryRefs(ascDateSortedEntryRefs)
    }

    // TODO: refactor and test algorithm in isolation!
    void indexEntryRefs(Collection<EntryRef> entryRefs) {

        def subscriptionPath = depot.getSubscriptionPath()

        // FIXME: skip entries already indexed!
        //  how? climb all? Wipe and re-index (thus ignoring these "get existing")?
        // .. wiping controlled from generateIndex, this method should then add
        //    only new -- see "assure added entries younger" below
        def currFeed = getFeed(subscriptionPath)
        if (currFeed == null) {
            currFeed = newFeed(subscriptionPath)
        }
        def youngestArchFeed = getPrevArchiveAsFeed(currFeed)

        // TODO: assure added entries are younger than latest in currFeed?
        // Currently, indexEntryRefs re-indexes *if the same* are added..
        // .. if not, fail or re-index? Flag for this? *May* be critical
        // (Business-logic may *never* allow adding "older" ones)!

        def batchCount = currFeed.getEntries().size()
        for (EntryRef entryRef : entryRefs) {
            batchCount++
            def depotEntry = depot.getEntry(entryRef.uriPath, !includeDeleted)

            if (batchCount > getFeedBatchSize()) { // save as archive
                Paging.setArchive(currFeed, true)
                def archPath = depot.pathToArchiveFeed(depotEntry.updated) // youngest entry..
                currFeed.getSelfLink().setHref(archPath)
                Paging.setCurrent(currFeed, subscriptionPath)
                if (youngestArchFeed) {
                    Paging.setNextArchive(youngestArchFeed,
                            uriPathFromFeed(currFeed))
                    writeFeed(youngestArchFeed) // re-write..
                }
                writeFeed(currFeed)
                youngestArchFeed = currFeed
                currFeed = newFeed(subscriptionPath)
                batchCount = 1 // current item ends up in the new feed
            }

            if (youngestArchFeed) {
                Paging.setPreviousArchive(currFeed,
                        uriPathFromFeed(youngestArchFeed))
            }

            logger.info "Indexing entry: <${depotEntry.id}> [${depotEntry.updated}]"

            indexEntry(currFeed, depotEntry)

        }
        writeFeed(currFeed) // as subscription feed

    }

    protected Feed newFeed(String uriPath) {
        def feed = Abdera.instance.newFeed()
        // FIXME: metadata for feed basics: skelFeed.clone()
        //feed.id = baseUri
        //feed.title = null
        feed.setUpdated(new Date()) // TODO: which utcDateTime?
        feed.addLink(uriPath, "self")
        return feed
    }

    protected Feed getFeed(String uriPath) {
        def file = new File(depot.baseDir, depot.toFeedFilePath(uriPath))
        if (!file.exists()) {
            return null
        }
        return Abdera.instance.parser.parse(new FileInputStream(file)).root
    }

    protected Feed getPrevArchiveAsFeed(Feed feed) {
        def prev = Paging.getNextArchive(feed)
        if (!prev) {
            return null
        }
        return getFeed(prev.toString())
    }

    protected Feed getFeedForDateTime(Date date) {
        // TODO: to use for e.g. "emptying" deleted entries
        // - search in feed folder by date, time; opt. offset (can there be many in same same instant?)
        // .. getFeedForDateTime(depot.pathToArchiveFeed(date))
        return null
    }

    protected String uriPathFromFeed(Feed feed) {
        return feed.getSelfLink().getHref().toString()
    }

    protected void writeFeed(Feed feed) {
        def uriPath = uriPathFromFeed(feed)
        logger.info "Writing feed: <${uriPath}>"
        def feedFileName = depot.toFeedFilePath(uriPath)
        feed.writeTo(new FileOutputStream(new File(depot.baseDir, feedFileName)))
    }

    protected void indexEntry(Feed feed, DepotEntry depotEntry) {
        if (depotEntry.isDeleted()) {
            if (useTombstones) {
                def delElem = feed.addExtension(FEED_EXT_TOMBSTONE)
                delElem.setAttributeValue(
                        "ref", depotEntry.getId().toString())
                delElem.setAttributeValue(
                        "when", new AtomDate(depotEntry.getUpdated()).value)
            }
            /* TODO: Unless generating new (when we know all, including deleteds..)
                If so, historical entries must know if their current is deleted!
            dryOutHistoricalEntries(depotEntry)
            */
        }
        /* TODO: Ensure this insert is only done if atomEntry represents
            a deletion in itself (how to combine with addTombstone?).
        if (useDeletedEntriesInFeed) ...
        */
        def atomEntry = generateAtomEntryContent(depotEntry, false)
        feed.insertEntry(atomEntry)
    }

    //== Entry Specifics ==

    Entry generateAtomEntryContent(DepotEntry depotEntry, boolean force=true) {
        def entryFile = depotEntry.newContentFile(ATOM_ENTRY_MEDIA_TYPE)
        if (!force &&
            entryFile.isFile() &&
            entryFile.lastModified() > depotEntry.lastModified()) {
            return Abdera.instance.parser.parse(
                    new FileInputStream(entryFile)).root
        }
        def atomEntry = createAtomEntry(depotEntry)
        atomEntry.writeTo(new FileOutputStream(entryFile))
        return atomEntry
    }

    protected Entry createAtomEntry(DepotEntry depotEntry) {

        def atomEntry = Abdera.instance.newEntry()
        if (useLinkExtensionsMd5) {
            atomEntry.declareNS(LINK_EXT_MD5.namespaceURI, LINK_EXT_MD5.prefix)
        }

        atomEntry.id = depotEntry.getId()
        atomEntry.setUpdated(depotEntry.getUpdated())
        def publDate = depotEntry.getPublished()
        if (publDate) {
            atomEntry.setPublished(publDate)
        }

        if (useFeedSync) {
            // TODO: paint history
        }

        if (depotEntry.isDeleted()) {
            markAsDeleted(atomEntry)
            return atomEntry
        }

        // TODO: what to use as values?
        atomEntry.setTitle("")//getId().toString())
        atomEntry.setSummary("")//getId().toString())

        if (useEntrySelfLink) {
            def selfUriPath = depot.pathProcessor.makeNegotiatedUriPath(
                    depotEntry.getEntryUriPath(), ATOM_ENTRY_MEDIA_TYPE)
            atomEntry.addLink(selfUriPath, "self")
        }

        addContents(depotEntry, atomEntry)
        addEnclosures(depotEntry, atomEntry)

        return atomEntry
    }

    protected void markAsDeleted(Entry atomEntry) {
        if (useGdataDeleted) {
            atomEntry.addExtension(ENTRY_EXT_GDATA_DELETED)
        }
        if (useFeedSync) {
            SharingHelper.deleteEntry(atomEntry, "") // TODO: by..
        }
        atomEntry.setTitle("")
        atomEntry.setContent("")
    }

    protected void addContents(DepotEntry depotEntry, Entry atomEntry) {
        def contentIsSet = false
        for (content in depotEntry.findContents()) {
            if (content.mediaType == ATOM_ENTRY_MEDIA_TYPE) {
                continue
            }
            def ref
            if (!contentIsSet
                && content.mediaType == depotEntry.contentMediaType
                && content.lang == depotEntry.contentLanguage) {
                ref = atomEntry.setContent(new IRI(content.depotUriPath),
                        content.mediaType)
                if (content.lang) {
                    atomEntry.contentElement.language = content.lang
                }
                contentIsSet = true
            } else {
                ref = atomEntry.addLink(content.depotUriPath,
                        "alternate",
                        content.mediaType,
                        null, // title
                        content.lang,
                        content.file.length())
            }
            if (useLinkExtensionsMd5) {
                ref.setAttributeValue(LINK_EXT_MD5, content.md5Hex)
            }
        }
    }

    protected void addEnclosures(DepotEntry depotEntry, Entry atomEntry) {
        for (enclContent in depotEntry.findEnclosures()) {
            def link = atomEntry.addLink(enclContent.depotUriPath,
                    "enclosure",
                    enclContent.mediaType,
                    null, // title
                    enclContent.lang,
                    enclContent.file.length())
            if (useLinkExtensionsMd5) {
                link.setAttributeValue(LINK_EXT_MD5, enclContent.md5Hex)
            }
        }
    }

}

protected class EntryRef {
    String uriPath
    Date date
}
