package se.lagrummet.rinfo.main.storage
//use with MyClass singleton = MyClass.instance
@Singleton
class ReCollectQueue {
    private def failedQueue = [] as Queue
    public boolean isEmpty() {
        return failedQueue.isEmpty()
    }
    public def add(FailedResource failedResource) {
        if(!failedQueue.any { it.url == failedResource.url})
            failedQueue << failedResource
        else {
            def failed = failedQueue.find {it.url == failedResource.url } as FailedResource
            failed.numberOfRetries += 1
            failedQueue.removeAll { it.url == failed.url }
            failedQueue << failed
        }
        applyLimit()
    }

    public FailedResource peek() {
        return failedQueue.peek() as FailedResource
    }

    public FailedResource poll() {
        return failedQueue.poll() as FailedResource
    }

    private def applyLimit() {
        failedQueue.removeAll { it.numberOfRetries > 3}
    }

}

class FailedResource { def url, mimeType, lang, enclosedUriPath, numberOfRetries }