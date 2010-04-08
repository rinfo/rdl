package se.lagrummet.rinfo.collector.atom;

import java.util.*;

import org.apache.abdera.i18n.iri.IRI;


/**
 * Minimal interface to a persisted index of all entry ID:s known for a given
 * feed ID. Used if an encountered feed is marked as <em>complete</em>,
 * according to <a href="http://tools.ietf.org/html/rfc5005#section-2">RFC
 * 5005: Feed Paging and Archiving, section 2</a>.
 */
public interface CompleteFeedEntryIdIndex {

    /**
     * Must return id:s for all previously collected entries from a feed with
     * the given id. This list will be compared against the currently collected
     * feed to determine which entries are to be updated, and if any entries
     * are to be deleted (i.e. any id in the returned collection which is
     * missing in the currently collected feed).
     */
    Set<IRI> getEntryIdsForCompleteFeedId(IRI feedId);

    // TODO: two-step: set, [process], commit? Or granular:
    // addCollectedEntyId(IRI feedId, IRI entryId)
    // removeCollectedEntyId(IRI feedId, IRI entryId)
    void storeEntryIdsForCompleteFeedId(IRI feedId, Set<IRI> entryIds);

}
