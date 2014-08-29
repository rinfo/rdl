package se.lagrummet.rinfo.main.storage

import org.apache.abdera.Abdera
import spock.lang.Specification

class ReCollectQueueSpec extends Specification {

    def setupSpec() {
        ReCollectQueue.instance.purgeQueue()
    }
    def "It should be possible to add things to the queue"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost")

        when:
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
        then:
            queue.isEmpty() == false
    }

    def "It should be possible to peek the queue"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost")
        when:
            def failed = queue.peek()
        then:
            failed.contentEntry.getId().toString() == "http://localhost"
    }

    def "It should be possible to check the size of the queue"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost")
        when:
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
        then:
            queue.size() == 1
    }

    def "It should be possible to purge the queue"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost")
        when:
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
            queue.purgeQueue()
        then:
            queue.isEmpty() == true
    }

    def "It when adding an existing url to the queue it should requeue it and update the retries"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost")
        when:
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
        then:
            queue.peek().numberOfRetries == 1
    }

    def "Entities with more then 3 retries should be deleted"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost")
        when:
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
        then:
            queue.isEmpty() == true
    }

    def "It should be possible to poll the queue"() {
        setup:
            def queue = ReCollectQueue.instance
            def entry = Abdera.instance.newEntry()
            entry.setId("http://localhost1")
            def entry2 = Abdera.instance.newEntry()
            entry2.setId("http://localhost2")
        when:
            queue.add(new FailedEntry(contentEntry: entry, numberOfRetries: 0))
            queue.add(new FailedEntry(contentEntry: entry2, numberOfRetries: 0))
            def failed1 = queue.poll()
            def failed2 = queue.poll()
        then:
            failed1.contentEntry.getId().toString() == "http://localhost1"
            failed2.contentEntry.getId().toString() == "http://localhost2"
            queue.isEmpty() == true
    }
}