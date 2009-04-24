package se.lagrummet.rinfo.store.depot

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*

import org.apache.abdera.Abdera
import org.apache.abdera.model.AtomDate


class FileDepotAtomIndexTest extends FileDepotTempBase {

    @Test
    void shouldGenerateAtomEntry() {
        def entry = fileDepot.getEntry("/publ/1901/100")
        assertEquals 0, entry.findContents(fileDepot.pathHandler.
                hintForMediaType("application/atom+xml;type=entry")).size()
        fileDepot.onEntryModified(entry)
        def atomContent = entry.findContents("application/atom+xml;type=entry")[0]
        assert atomContent.file.isFile()
        // TODO: specify content, alternatives, enclosures, size, md5(?)
    }

    // TODO: shouldGenerateAtomEntryWhenIndexingNewEntry / or WhenCreating?
    // TODO: shouldGenerateAtomEntryWhenModified

    private indexEntryInNewFeed(atomizer, entry) {
        def feed = Abdera.getInstance().newFeed()
        atomizer.indexEntry(feed, entry)
        return feed
    }

    private getDeletedEntry() {
        return fileDepot.getUncheckedDepotEntry("/publ/1901/0")
    }

    @Test
    void shouldIfConfiguredUseEntriesAsTombstones() {
        def entry = getDeletedEntry()

        def indexAndGetAtomEntry = { atomizer ->
            def feed = indexEntryInNewFeed(atomizer, entry)
            def found = feed.getEntry(entry.id as String)
            if (!found || found.updated != entry.updated) {
                return null
            }
            return found
        }

        assertNotNull indexAndGetAtomEntry(new Atomizer(
            useTombstones:false,
            useFeedSync:true,
            useGdataDeleted:false,
        ))

        assertNotNull indexAndGetAtomEntry(new Atomizer(
            useTombstones:false,
            useFeedSync:false,
            useGdataDeleted:true,
        ))

        assertNull indexAndGetAtomEntry(new Atomizer(
            useTombstones:true,
            useFeedSync:false,
            useGdataDeleted:false,
        ))

        assertNull indexAndGetAtomEntry(new Atomizer(
            useTombstones:false,
            useFeedSync:false,
            useGdataDeleted:false,
        ))
    }

    @Test
    void shouldIfConfiguredOnlyUseFeedLevelTombstones() {
        def entry = getDeletedEntry()
        def feed = indexEntryInNewFeed(new Atomizer(
            useTombstones:true,
            useFeedSync:false,
            useGdataDeleted:false,
        ), entry)

        assertNull feed.getEntry(entry.id as String)

        def tombstones = feed.getExtensions(Atomizer.FEED_EXT_TOMBSTONE
                ).collect {
            [
                ref: it.getAttributeValue(Atomizer.TOMBSTONE_REF),
                when: new AtomDate(
                        it.getAttributeValue(Atomizer.TOMBSTONE_WHEN)).date
            ]
        }
        assertNotNull tombstones.find {
            it.ref == entry.id as String && it.when == entry.updated
        }
    }

}
