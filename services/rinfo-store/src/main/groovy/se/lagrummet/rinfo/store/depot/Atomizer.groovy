package se.lagrummet.rinfo.store.depot

import org.slf4j.LoggerFactory

import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry
import org.apache.abdera.i18n.iri.IRI

import org.apache.abdera.ext.history.FeedPagingHelper as Paging


class Atomizer {

    private final logger = LoggerFactory.getLogger(Atomizer)

    // TODO: must be in Abdera..
    static final ATOM_ENTRY_MEDIA_TYPE = "application/atom+xml;type=entry"

    FileDepot depot

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
        for (depotEntry in depot.iterateEntries()) {
            ascDateSortedEntryRefs.add(
                    new EntryRef(uriPath: depotEntry.entryUriPath,
                            date: depotEntry.updated) )
        }
        indexEntryRefs(ascDateSortedEntryRefs)
    }

    // TODO: refactor and test algorithm in isolation?
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
            def depotEntry = depot.getEntry(entryRef.uriPath)

            if (batchCount > getFeedBatchSize()) { // save as archive
                batchCount = 1 // popping added, but we have one new
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

    protected void indexEntry(Feed feed, DepotEntry depotEntry) {
        // FIXME: handle depotEntry.deleted ... !
        def entryFile = generateAtomEntryContent(depotEntry, false)
        def atomEntry = Abdera.instance.parser.parse(
                new FileInputStream(entryFile)).root
        feed.insertEntry(atomEntry)
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

    //== Entry Specifics ==

    File generateAtomEntryContent(DepotEntry depotEntry, boolean force=true) {
        def entryFile = depotEntry.newContentFile(ATOM_ENTRY_MEDIA_TYPE)
        if (!force &&
            entryFile.isFile() &&
            entryFile.lastModified() > depotEntry.lastModified()) {
           return entryFile
        }
        createAtomEntry(depotEntry).writeTo(new FileOutputStream(entryFile))
        return entryFile
    }

    Entry createAtomEntry(DepotEntry depotEntry) {

        // TODO: how to represent deleted tombstones!
        def atomEntry = Abdera.instance.newEntry()
        // TODO: getEntryManifest().clone() ?
        atomEntry.id = depotEntry.getId()
        def publDate = depotEntry.getPublished()
        if (publDate) {
            atomEntry.setPublished(publDate)
        }
        atomEntry.setUpdated(depotEntry.getUpdated())

        // TODO: what to use as values?
        atomEntry.setTitle("")//getId().toString())
        atomEntry.setSummary("")//getId().toString())

        def selfUriPath = depot.pathProcessor.makeNegotiatedUriPath(
                depotEntry.getEntryUriPath(), ATOM_ENTRY_MEDIA_TYPE)
        atomEntry.addLink(selfUriPath, "self")

        // TODO: add md5 (link extension) or sha (xml-dsig)?

        def contentIsSet = false
        for (content in depotEntry.findContents()) {
            if (content.mediaType == ATOM_ENTRY_MEDIA_TYPE) {
                continue
            }
            if (!contentIsSet
                && content.mediaType == depotEntry.contentMediaType
                && content.lang == depotEntry.contentLanguage) {
                atomEntry.setContent(new IRI(content.depotUriPath),
                        content.mediaType)
                if (content.lang) {
                    atomEntry.contentElement.language = content.lang
                }
                contentIsSet = true
            } else {
                atomEntry.addLink(content.depotUriPath,
                        "alternate",
                        content.mediaType,
                        null, // title
                        content.lang,
                        content.file.length())
            }
        }

        for (enclContent in depotEntry.findEnclosures()) {
            atomEntry.addLink(enclContent.depotUriPath,
                    "enclosure",
                    enclContent.mediaType,
                    null, // title
                    enclContent.lang,
                    enclContent.file.length())
        }

        return atomEntry
    }

}

protected class EntryRef {
    String uriPath
    Date date
}
