package se.lagrummet.rinfo.main.storage
import org.apache.abdera.model.Entry;
//use with MyClass singleton = MyClass.instance
@Singleton
class ReCollectQueue {
    private def failedQueue = [] as Queue
    public boolean isEmpty() {
        return failedQueue.isEmpty()
    }
    public def add(FailedEntry failedEntry) {
        if(!failedQueue.any { it.contentEntry?.id == failedEntry.contentEntry.id})
            failedQueue << failedEntry
        else {
            def failed = failedQueue.find {it.contentEntry.id == failedEntry.contentEntry.id } as FailedEntry
            failed.numberOfRetries += 1
            failedQueue.removeAll { it.contentEntry.id == failed.contentEntry.id }
            failedQueue << failed
        }
        applyLimit()
    }

    public FailedEntry peek() {
        return failedQueue.peek() as FailedEntry
    }

    public FailedEntry poll() {
        return failedQueue.poll() as FailedEntry
    }

    public def getAsList() {
        return failedQueue as List
    }

    public def size() {
        return failedQueue.size()
    }

    public def purgeQueue() {
        return failedQueue.removeAll(failedQueue)
    }

    private def applyLimit() {
        failedQueue.removeAll { it.numberOfRetries > 3}
    }


}

class FailedEntry {
    def numberOfRetries = 0
    Entry contentEntry
}