package se.lagrummet.rinfo.main.storage
import org.apache.abdera.model.Entry
import org.slf4j.Logger
import org.slf4j.LoggerFactory;
//use with MyClass singleton = MyClass.instance
@Singleton
class ReCollectQueue {
    private final Logger logger = LoggerFactory.getLogger(ReCollectQueue.class)

    private def failedQueue = [] as Queue
    public boolean isEmpty() {
        return failedQueue.isEmpty()
    }
    public def add(FailedEntry failedEntry) {
        if(!failedEntry.contentEntry) {
            logger.error("there MUST be an entry to recollect.")
            return
        }
        logger.debug("Adding ${failedEntry.contentEntry?.id}")
        if(!failedQueue.any { it.contentEntry?.id == failedEntry.contentEntry.id})
            failedQueue << failedEntry
        else {
            def failed = failedQueue.find {it.contentEntry.id == failedEntry.contentEntry.id } as FailedEntry
            failed.numberOfRetries += 1
            logger.debug("${failedEntry.contentEntry?.id} already exists in queue, increasing number of tries (now ${failed.numberOfRetries}) and requeue")
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
        logger.info("Purging the whole recollect queue")
        return failedQueue.removeAll(failedQueue)
    }

    private def applyLimit() {
        failedQueue.removeAll {
            if(it.numberOfRetries > 3) {
                logger.info("Too many retries, removing ${it.contentEntry.id} from the queue")
                return true
            }
            return false
        }
    }


}

class FailedEntry {
    def numberOfRetries = 0
    Entry contentEntry
}