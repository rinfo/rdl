package se.lagrummet.rinfo.main.storage

import spock.lang.*

class ReCollectQueueSpec extends Specification {

    def "It should be possible to add things to the queue"() {
        setup:
            def queue = ReCollectQueue.instance
        when:
            queue.add(new FailedResource(url: "http://localhost", numberOfRetries: 0))
        then:
            queue.isEmpty() == false
    }

    def "It should be possible to peek the queue"() {
        setup:
            def queue = ReCollectQueue.instance
        when:
            def failed = queue.peek()
        then:
            failed.url == "http://localhost"
    }

    def "It when adding an existing url to the queue it should requeue it and update the retries"() {
        setup:
            def queue = ReCollectQueue.instance
        when:
            queue.add(new FailedResource(url: "http://localhost", numberOfRetries: 0))
        then:
            queue.peek().numberOfRetries == 1
    }

    def "Entities with more then 3 retries should be deleted"() {
        setup:
            def queue = ReCollectQueue.instance
        when:
            queue.add(new FailedResource(url: "http://localhost", numberOfRetries: 0))
            queue.add(new FailedResource(url: "http://localhost", numberOfRetries: 0))
            queue.add(new FailedResource(url: "http://localhost", numberOfRetries: 0))
        then:
            queue.isEmpty() == true
    }

    def "It should be possible to poll the queue"() {
        setup:
            def queue = ReCollectQueue.instance
        when:
            queue.add(new FailedResource(url: "http://localhost1", numberOfRetries: 0))
            queue.add(new FailedResource(url: "http://localhost2", numberOfRetries: 0))
            def failed1 = queue.poll()
            def failed2 = queue.poll()
        then:
            failed1.url == "http://localhost1"
            failed2.url == "http://localhost2"
            queue.isEmpty() == true
    }
}