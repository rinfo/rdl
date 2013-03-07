package se.lagrummet.rinfo.collector.atom

import org.apache.abdera.model.AtomDate
import org.apache.abdera.i18n.iri.IRI


class FeedEntryDataIndexMemImpl implements FeedEntryDataIndex {

    def feedEntryDataMap = [:]

    Map<IRI, AtomDate> getEntryDataForCompleteFeedId(IRI feedId) {
        return feedEntryDataMap[feedId]
    }

    void storeEntryDataForCompleteFeedId(IRI feedId,
            Map<IRI, AtomDate> entryData) {
        feedEntryDataMap[feedId] = entryData
    }

}
