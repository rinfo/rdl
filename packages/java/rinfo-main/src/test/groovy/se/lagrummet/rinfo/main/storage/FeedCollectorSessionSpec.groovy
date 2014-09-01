package se.lagrummet.rinfo.main.storage

import org.apache.abdera.Abdera
import org.apache.http.impl.client.DefaultHttpClient
import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.DepotSession
import se.lagrummet.rinfo.store.depot.FileDepot
import spock.lang.Specification
import org.apache.http.client.HttpClient
import org.openrdf.repository.Repository

class FeedCollectorSessionSpec extends Specification {
    def logsession = Mock(CollectorLogSession)
    def "Should add failed entries to recollectqueue"() {
        setup:
            def feed = Abdera.instance.newFeed()
            def entryThatWillFail = Abdera.instance.newEntry()
            def content = Abdera.instance.getFactory().newContent()
            //def logsession = Mock(CollectorLogSession)

            feed.addEntry(entryThatWillFail)
            entryThatWillFail.setId("http://willfail")
            entryThatWillFail.setContentElement(content)
            logsession.logError(_,_,_,_) >> ErrorAction.CONTINUEANDRETRYLATER
            content.setSrc("http://localhost/fail")

            def ss = makeStorageSessionWithLogSession(Mock(Depot), Mock(DepotSession), [Mock(StorageHandler)], logsession, false)

            GroovyMock(StorageSession, global: true)
            StorageSession.setViaEntry(_,_,_) >> { throw new FileNotFoundException() }


            def fcs = new FeedCollectorSession(new DefaultHttpClient(), ss)
        when:
            fcs.processFeedPageInOrder(new URL("http://localhost"), feed, [entryThatWillFail], [:])
        then:
            ReCollectQueue.instance.peek().contentEntry.id == entryThatWillFail.id
    }

    def "when collecting the recollectfeed successful entrys should be removed from the queue"() {
        setup:
            def queue = ReCollectQueue.instance

            def entryThatWillSucceed = Abdera.instance.newEntry()
            def content = Abdera.instance.getFactory().newContent()


            entryThatWillSucceed.setId("http://willsucceed")
            entryThatWillSucceed.setContentElement(content)

            content.setSrc("http://localhost/yey")

            def sss = makeStorageSessionWithLogSession(Mock(Depot), Mock(DepotSession), [Mock(StorageHandler)], logsession, false)
            def fcs = new FeedCollectorSession(new DefaultHttpClient(), sss)
            queue.add(new FailedEntry(contentEntry: entryThatWillSucceed))
            GroovyMock(StorageSession, global: true)
            StorageSession.setViaEntry(_,_,_) >> { return }

        when:
            def feed = ReCollectFeed.generate(queue.asList)

            fcs.processFeedPageInOrder(new URL("http://localhost/feed/recollect"), feed, [entryThatWillSucceed], [:])
        then:
            !queue.asList.any {it.contentEntry.id == entryThatWillSucceed.id}

    }

    def "when collecting the recollectfeed unsuccessful entrys should be readded to the queue"() {
        setup:
            def queue = ReCollectQueue.instance

            def entryThatWillFail = Abdera.instance.newEntry()
            def content = Abdera.instance.getFactory().newContent()


            entryThatWillFail.setId("http://willfailagain")
            entryThatWillFail.setContentElement(content)

            logsession.logError(_,_,_,_) >> ErrorAction.CONTINUEANDRETRYLATER
            content.setSrc("http://localhost/fail")
            queue.add(new FailedEntry(contentEntry: entryThatWillFail))
            def ss = makeStorageSessionWithLogSession(Mock(Depot), Mock(DepotSession), [Mock(StorageHandler)], logsession, false)
            def fcs = new FeedCollectorSession(new DefaultHttpClient(), ss)

            GroovyMock(StorageSession, global: true)
            StorageSession.setViaEntry(_,_,_) >> { throw new FileNotFoundException() }
        when:
            def feed = ReCollectFeed.generate(queue.asList)

            fcs.processFeedPageInOrder(new URL("http://localhost/feed/recollect"), feed, [entryThatWillFail], [:])
        then:
            queue.asList.find { it.contentEntry.id == entryThatWillFail.id}?.numberOfRetries == 1
    }

    private makeStorageSessionWithLogSession(depot, depotSession, handlers, admin=false, logSession, isCheckerCollect) {
        depot.openSession() >> depotSession
        depotSession.getDepot() >> depot
        def sourceFeed = "http://example.org/feed"
        def credentials = new StorageCredentials(
                new CollectorSource(new URI(sourceFeed), new URL(sourceFeed)), admin)
        return new StorageSession(credentials, depotSession, handlers, logSession, null, isCheckerCollect)
    }

}
