package se.lagrummet.rinfo.main.storage

import spock.lang.*

import se.lagrummet.rinfo.store.depot.DepotSession
import se.lagrummet.rinfo.store.depot.DepotContent
import se.lagrummet.rinfo.store.depot.DepotEntry


class SourceFeedsConfigurationSpec extends Specification {

    def systemDatasetUri = new URI("http://example.org/dataset")
    def configurationEntryId = systemDatasetUri

    def sourceFeedUrls = [
        new URL("http://dom.se/dvfs/feed/current"),
        new URL("http://regeringen.se/sfs/feed/current"),
    ]

    //todo fix test /CM
/*
    def "Collect scheduler gets source feeds from rdf in configured entry"() {
        setup: "configure handler with entry id"
        def collectScheduler = new FeedCollectScheduler(null)
        def sourceFeedsConfigHandler = new SourceFeedsConfigHandler(
                collectScheduler, configurationEntryId, systemDatasetUri)
        def session = new StorageSession(new StorageCredentials(null, true),
                Mock(DepotSession), [], Mock(CollectorLogSession), null, ErrorLevel.WARNING)

        when: "an entry with expected id is created"
        sourceFeedsConfigHandler.onModified(session, mockSourcesEntry(), true)

        then: "the collect scheduler gets source feeds from the entry data"
        collectScheduler.sourceFeedUrls.toList().sort() == sourceFeedUrls
    }
*/

    /*def "Source feed entry must come from admin session"() {
        setup: "non-admin credentials"
        def sourceFeedsConfigHandler = new SourceFeedsConfigHandler(
                null, configurationEntryId, systemDatasetUri)
        def session = new StorageSession(new StorageCredentials(null, false),
                Mock(DepotSession), [], Mock(CollectorLogSession), null, ErrorLevel.WARNING)

        when: "an entry with expected id is created"
        sourceFeedsConfigHandler.onModified(session, mockSourcesEntry(), true)

        then: "config handler fails on non-admin credentials"
        thrown(Exception) // TODO: NotAllowedException..
    } */

    private mockSourcesEntry() {
        DepotEntry entry = Mock()
        entry.getId() >> configurationEntryId
        entry.getMimeType() >> "application/rdf+xml"
        entry.findContents() >> [
                new DepotContent(new File("src/test/resources/source_feeds.rdf"),
                    "${configurationEntryId}/rdf", "application/rdf+xml"),
            ]
        return entry
    }

}
